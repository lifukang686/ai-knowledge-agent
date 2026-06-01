package com.fukang.knowledge.agent.application.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fukang.knowledge.agent.application.agent.command.AgentCreateCommand;
import com.fukang.knowledge.agent.application.agent.port.AgentRepository;
import com.fukang.knowledge.agent.application.agent.port.AgentRunRepository;
import com.fukang.knowledge.agent.application.agent.port.AiServicesAgentRuntime;
import com.fukang.knowledge.agent.application.agent.result.AgentConfigResult;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.domain.agent.model.AgentRunEvent;
import com.fukang.knowledge.agent.domain.agent.model.AgentRunResult;
import com.fukang.knowledge.agent.domain.agent.model.AgentStep;
import com.fukang.knowledge.agent.domain.agent.model.AgentStepRecord;
import com.fukang.knowledge.agent.domain.agent.model.ExecutionStrategy;
import com.fukang.knowledge.agent.infrastructure.config.AgentProperties;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.AgentDO;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.AgentRunDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Agent 应用服务。
 * <p>统一承载 Agent 配置创建、Plan-Execute 执行、AiServices 执行和运行日志查询。
 * 两种执行策略都会输出 {@link AgentRunEvent}，便于前端用同一套结构展示运行详情。</p>
 */
@Slf4j
@Service
public class AgentAppService {

    private final AgentRepository agentRepository;
    private final AgentRunRepository agentRunRepository;
    private final PlanExecuteAgentRuntime planExecuteAgentRuntime;
    private final AiServicesAgentRuntime aiServicesAgentRuntime;
    private final AgentProperties agentProperties;
    /** 负责序列化 toolIds 和 agent_run.log 中的结构化事件。 */
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AgentAppService(AgentRepository agentRepository,
                           AgentRunRepository agentRunRepository,
                           PlanExecuteAgentRuntime planExecuteAgentRuntime,
                           AiServicesAgentRuntime aiServicesAgentRuntime,
                           AgentProperties agentProperties) {
        this.agentRepository = agentRepository;
        this.agentRunRepository = agentRunRepository;
        this.planExecuteAgentRuntime = planExecuteAgentRuntime;
        this.aiServicesAgentRuntime = aiServicesAgentRuntime;
        this.agentProperties = agentProperties;
    }

    @Transactional
    public AgentConfigResult create(AgentCreateCommand command) {
        if (command.name() == null || command.name().isBlank()) {
            throw new BaseException(ErrorCodeEnum.BAD_REQUEST.getCode(), "Agent name cannot be blank");
        }

        AgentDO agentDO = new AgentDO();
        agentDO.setName(command.name());
        agentDO.setDescription(command.description());
        agentDO.setToolIds(serializeToolIds(command.toolIds()));
        agentDO.setSystemPrompt(command.systemPrompt());
        agentDO.setMaxSteps(command.maxSteps());
        agentRepository.insert(agentDO);

        log.info("Agent created: id={}, name={}, toolCount={}",
                agentDO.getId(), agentDO.getName(), command.toolIds() != null ? command.toolIds().size() : 0);
        return toAgentConfigResult(agentDO, command.toolIds());
    }

    /**
     * 按指定策略执行 Agent。
     *
     * @param strategy 为空时默认使用 Plan-Execute
     */
    @Transactional
    public AgentRunResult run(Long agentId, String task, ExecutionStrategy strategy) {
        ExecutionStrategy execStrategy = strategy != null ? strategy : ExecutionStrategy.PLAN_EXECUTE;
        if (execStrategy == ExecutionStrategy.AI_SERVICES) {
            return runWithAiServices(agentId, task);
        }
        return runWithPlanExecute(agentId, task);
    }

    @Transactional
    public AgentRunResult run(Long agentId, String task) {
        return run(agentId, task, ExecutionStrategy.PLAN_EXECUTE);
    }

    /**
     * Plan-Execute 策略。
     * <p>流程为：生成计划 → Reasoner 判断下一步 → 调用工具 → 记录观察结果 → 循环直至最终答案。</p>
     */
    private AgentRunResult runWithPlanExecute(Long agentId, String task) {
        AgentDO agentDO = agentRepository.findById(agentId);
        if (agentDO == null) {
            throw new BaseException(ErrorCodeEnum.AGENT_NOT_EXIST);
        }

        int maxSteps = agentDO.getMaxSteps() != null ? agentDO.getMaxSteps() : agentProperties.getMaxSteps();
        AgentRunDO runDO = createRunRecord(agentId, task);
        long startTime = System.currentTimeMillis();

        try {
            PlanExecuteAgentRuntime.RuntimeResult result = planExecuteAgentRuntime.runTask(task,
                    AgentRuntimeOptions.of(maxSteps, "", null));
            updateRunRecord(runDO, result.status(), result.answer(), result.events());
            return buildResult(runDO.getId(), result.answer(), result.status(), result.steps(),
                    result.events(), result.totalDurationMs());
        } catch (Exception e) {
            log.error("Agent execution failed: runId={}, agentId={}", runDO.getId(), agentId, e);
            List<AgentRunEvent> events = List.of(event(AgentRunEvent.EventType.ERROR, null, null,
                    Map.of("reason", e.getClass().getSimpleName()), false, null,
                    "System error: " + e.getMessage()));
            updateRunRecord(runDO, "FAILED", "System error: " + e.getMessage(), events);
            return buildResult(runDO.getId(), "System error: " + e.getMessage(), "FAILED",
                    List.of(), events, System.currentTimeMillis() - startTime);
        }
    }

    public AgentRunResult getRunStatus(Long runId) {
        AgentRunDO runDO = agentRunRepository.findById(runId);
        if (runDO == null) {
            throw new BaseException(ErrorCodeEnum.AGENT_RUN_NOT_EXIST);
        }
        List<AgentRunEvent> events = parseEvents(runDO.getLog());
        List<AgentStep> steps = eventsToSteps(events);
        return buildResult(runId, runDO.getOutputAnswer(), runDO.getStatus(),
                steps, events, calculateDuration(runDO.getStartTime(), runDO.getEndTime()));
    }

    @Transactional
    public AgentRunResult runWithAiServices(Long agentId, String task) {
        AgentDO agentDO = agentRepository.findById(agentId);
        if (agentDO == null) {
            throw new BaseException(ErrorCodeEnum.AGENT_NOT_EXIST);
        }

        AgentRunDO runDO = createRunRecord(agentId, task);
        runDO.setStatus("EXECUTING");
        agentRunRepository.updateById(runDO);

        List<AgentRunEvent> events = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        try {
            String systemPrompt = agentDO.getSystemPrompt() != null && !agentDO.getSystemPrompt().isBlank()
                    ? agentDO.getSystemPrompt()
                    : "You are an intelligent assistant. Use tools when needed to complete the task.";
            AiServicesAgentRuntime.ExecutionResult executionResult =
                    aiServicesAgentRuntime.execute(task, systemPrompt);
            String answer = executionResult.answer();
            events.addAll(executionResult.events());
            events.add(event(AgentRunEvent.EventType.FINAL_ANSWER, null, null,
                    Map.of("answer", answer), true, null, "Final answer"));

            long totalDuration = System.currentTimeMillis() - startTime;
            updateRunRecord(runDO, "COMPLETED", answer, events);
            return buildResult(runDO.getId(), answer, "COMPLETED", eventsToSteps(events), events, totalDuration);
        } catch (Exception e) {
            log.error("AiServices agent execution failed: runId={}, agentId={}", runDO.getId(), agentId, e);
            String errorMsg = "Execution error: " + e.getMessage();
            events.add(event(AgentRunEvent.EventType.ERROR, null, null,
                    Map.of("exception", e.getClass().getSimpleName()), false, null, errorMsg));
            long totalDuration = System.currentTimeMillis() - startTime;
            updateRunRecord(runDO, "FAILED", errorMsg, events);
            return buildResult(runDO.getId(), errorMsg, "FAILED", eventsToSteps(events), events, totalDuration);
        }
    }

    private AgentRunDO createRunRecord(Long agentId, String task) {
        AgentRunDO runDO = new AgentRunDO();
        runDO.setAgentId(agentId);
        runDO.setInputQuery(task);
        runDO.setStatus("PLANNING");
        runDO.setStartTime(LocalDateTime.now());
        runDO.setCreateTime(LocalDateTime.now());
        agentRunRepository.insert(runDO);
        return runDO;
    }

    private void updateRunRecord(AgentRunDO runDO, String status, String output, List<AgentRunEvent> events) {
        runDO.setStatus(status);
        runDO.setOutputAnswer(output);
        runDO.setEndTime(LocalDateTime.now());
        runDO.setLog(serializeEvents(events));
        agentRunRepository.updateById(runDO);
    }

    private AgentRunEvent event(AgentRunEvent.EventType type, Integer stepOrder, String toolName,
                                Map<String, Object> payload, Boolean success,
                                Long durationMs, String message) {
        return AgentRunEvent.of(type, stepOrder, toolName, payload, success, durationMs, message);
    }

    private String serializeEvents(List<AgentRunEvent> events) {
        try {
            return objectMapper.writeValueAsString(events);
        } catch (Exception e) {
            log.error("Failed to serialize agent run events", e);
            return "[]";
        }
    }

    private List<AgentRunEvent> parseEvents(String logJson) {
        if (logJson == null || logJson.isBlank()) {
            return List.of();
        }
        try {
            List<AgentRunEvent> events = objectMapper.readValue(logJson, new TypeReference<List<AgentRunEvent>>() {});
            if (events.isEmpty() || events.get(0).type() != null) {
                return events;
            }
        } catch (Exception e) {
            log.debug("Agent run log is not event format, trying legacy step format");
        }
        // 兼容旧版本 agent_run.log 中直接存 AgentStep 列表的历史数据。
        try {
            List<AgentStep> legacySteps = objectMapper.readValue(logJson, new TypeReference<List<AgentStep>>() {});
            return stepsToEvents(legacySteps);
        } catch (Exception e) {
            log.error("Failed to parse agent run log", e);
            return List.of();
        }
    }

    private List<AgentStep> eventsToSteps(List<AgentRunEvent> events) {
        // steps 是旧前端兼容字段，只从 observation 事件派生。
        return events.stream()
                .filter(e -> AgentRunEvent.EventType.OBSERVATION.code().equals(e.type()))
                .map(e -> new AgentStep(
                        e.stepOrder(),
                        e.toolName(),
                        Map.of(),
                        stringPayload(e, "result", e.message()),
                        e.durationMs(),
                        e.success(),
                        stringPayload(e, "errorMessage", null)
                ))
                .toList();
    }

    private List<AgentRunEvent> stepsToEvents(List<AgentStep> steps) {
        return steps.stream()
                .map(step -> event(AgentRunEvent.EventType.OBSERVATION,
                        step.stepOrder(), step.toolName(),
                        Map.of("result", step.observation() != null ? step.observation() : "",
                                "errorMessage", step.errorMessage() != null ? step.errorMessage() : ""),
                        step.success(), step.durationMs(), "Legacy step observation"))
                .toList();
    }

    private String stringPayload(AgentRunEvent event, String key, String defaultValue) {
        Object value = event.payload() != null ? event.payload().get(key) : null;
        return value != null ? String.valueOf(value) : defaultValue;
    }

    private long calculateDuration(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        return Duration.between(start, end).toMillis();
    }

    private AgentRunResult buildResult(Long runId, String result, String status,
                                       List<AgentStep> steps, List<AgentRunEvent> events,
                                       long totalDuration) {
        List<AgentStepRecord> stepRecords = steps.stream()
                .map(s -> new AgentStepRecord(
                        s.stepOrder() != null ? s.stepOrder() : 0,
                        s.toolName(),
                        s.observation() != null ? s.observation() : "",
                        Boolean.TRUE.equals(s.success()),
                        s.durationMs() != null ? s.durationMs() : 0
                ))
                .toList();
        return new AgentRunResult(runId, result, status, stepRecords, events, totalDuration);
    }

    private String serializeToolIds(List<Long> toolIds) {
        if (toolIds == null || toolIds.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(toolIds);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize agent tool ids", e);
            return "[]";
        }
    }

    private AgentConfigResult toAgentConfigResult(AgentDO agentDO, List<Long> toolIds) {
        return new AgentConfigResult(agentDO.getId(), agentDO.getName(),
                agentDO.getDescription(), toolIds != null ? toolIds : List.of(), agentDO.getSystemPrompt(),
                agentDO.getMaxSteps(), agentDO.getCreateTime() != null
                        ? agentDO.getCreateTime().toString() : null);
    }
}
