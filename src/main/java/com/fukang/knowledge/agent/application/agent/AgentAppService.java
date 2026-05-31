package com.fukang.knowledge.agent.application.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fukang.knowledge.agent.api.agent.dto.AgentCreateReq;
import com.fukang.knowledge.agent.api.agent.dto.AgentResp;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.enums.ModelTypeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.domain.agent.model.AgentContext;
import com.fukang.knowledge.agent.domain.agent.model.AgentRunEvent;
import com.fukang.knowledge.agent.domain.agent.model.AgentRunResult;
import com.fukang.knowledge.agent.domain.agent.model.AgentStep;
import com.fukang.knowledge.agent.domain.agent.model.AgentStepRecord;
import com.fukang.knowledge.agent.domain.agent.model.ExecutionStrategy;
import com.fukang.knowledge.agent.domain.agent.model.Observation;
import com.fukang.knowledge.agent.domain.agent.model.PlanStep;
import com.fukang.knowledge.agent.domain.agent.model.ReasoningResult;
import com.fukang.knowledge.agent.infrastructure.ai.AgentMemoryFactory;
import com.fukang.knowledge.agent.infrastructure.ai.DynamicModelManager;
import com.fukang.knowledge.agent.infrastructure.config.AgentProperties;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.AgentDO;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.AgentRunDO;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.AgentMapper;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.AgentRunMapper;
import com.fukang.knowledge.agent.infrastructure.tool.DynamicToolProvider;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AgentAppService {

    private final AgentMapper agentMapper;
    private final AgentRunMapper agentRunMapper;
    private final AgentPlanner agentPlanner;
    private final AgentExecutor agentExecutor;
    private final AgentReasoner agentReasoner;
    private final DynamicToolProvider dynamicToolProvider;
    private final DynamicModelManager dynamicModelManager;
    private final AgentMemoryFactory memoryFactory;
    private final AgentProperties agentProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AgentAppService(AgentMapper agentMapper,
                           AgentRunMapper agentRunMapper,
                           AgentPlanner agentPlanner,
                           AgentExecutor agentExecutor,
                           AgentReasoner agentReasoner,
                           DynamicToolProvider dynamicToolProvider,
                           DynamicModelManager dynamicModelManager,
                           AgentMemoryFactory memoryFactory,
                           AgentProperties agentProperties) {
        this.agentMapper = agentMapper;
        this.agentRunMapper = agentRunMapper;
        this.agentPlanner = agentPlanner;
        this.agentExecutor = agentExecutor;
        this.agentReasoner = agentReasoner;
        this.dynamicToolProvider = dynamicToolProvider;
        this.dynamicModelManager = dynamicModelManager;
        this.memoryFactory = memoryFactory;
        this.agentProperties = agentProperties;
    }

    @Transactional
    public AgentResp create(AgentCreateReq req) {
        if (req.name() == null || req.name().isBlank()) {
            throw new BaseException(ErrorCodeEnum.BAD_REQUEST.getCode(), "Agent name cannot be blank");
        }

        AgentDO agentDO = new AgentDO();
        agentDO.setName(req.name());
        agentDO.setDescription(req.description());
        agentDO.setToolIds(serializeToolIds(req.toolIds()));
        agentDO.setSystemPrompt(req.systemPrompt());
        agentDO.setMaxSteps(req.maxSteps());
        agentMapper.insert(agentDO);

        log.info("Agent created: id={}, name={}, toolCount={}",
                agentDO.getId(), agentDO.getName(), req.toolIds() != null ? req.toolIds().size() : 0);
        return toAgentResp(agentDO, req.toolIds());
    }

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

    private AgentRunResult runWithPlanExecute(Long agentId, String task) {
        AgentDO agentDO = agentMapper.selectById(agentId);
        if (agentDO == null) {
            throw new BaseException(ErrorCodeEnum.AGENT_NOT_EXIST);
        }

        int maxSteps = agentDO.getMaxSteps() != null ? agentDO.getMaxSteps() : agentProperties.getMaxSteps();
        AgentRunDO runDO = createRunRecord(agentId, task);
        AgentContext context = new AgentContext(task);
        context.setChatMemory(memoryFactory.createDefault());
        List<AgentRunEvent> events = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        try {
            context.setStatus(AgentContext.AgentContextStatus.PLANNING);
            List<PlanStep> plan = agentPlanner.plan(task);
            context.setPlanSteps(plan);
            events.add(event(AgentRunEvent.EventType.PLAN, null, null,
                    Map.of("steps", plan), true, null, "Plan generated"));

            context.setStatus(AgentContext.AgentContextStatus.EXECUTING);
            int stepCount = 0;
            while (stepCount < maxSteps) {
                ReasoningResult reasoning = agentReasoner.reason(context);
                recordReasoning(events, reasoning, "Reasoning decision");

                if (reasoning.decision() == ReasoningResult.Decision.FINAL_ANSWER) {
                    return completeRun(runDO, context, events, reasoning.content(), startTime);
                }
                if (reasoning.decision() == ReasoningResult.Decision.ABORT) {
                    return failRun(runDO, context, events, reasoning.content(), startTime, "Agent aborted");
                }
                if (reasoning.decision() == ReasoningResult.Decision.RETRY) {
                    AgentStep lastStep = context.getLastStep();
                    if (lastStep != null) {
                        PlanStep retryStep = new PlanStep(lastStep.stepOrder(),
                                lastStep.toolName(), lastStep.parameters(), "Retry previous step");
                        executeAndRecord(context, retryStep, events);
                        stepCount++;
                    }
                    continue;
                }

                if (context.getRemainingSteps().isEmpty()) {
                    ReasoningResult finalReason = agentReasoner.reason(context);
                    recordReasoning(events, finalReason, "Final reasoning decision");
                    if (finalReason.decision() == ReasoningResult.Decision.FINAL_ANSWER) {
                        return completeRun(runDO, context, events, finalReason.content(), startTime);
                    }
                    break;
                }

                PlanStep nextStep = context.getRemainingSteps().get(0);
                executeAndRecord(context, nextStep, events);
                stepCount++;
            }

            String errorMsg = "Agent reached max steps limit: " + maxSteps;
            return failRun(runDO, context, events, errorMsg, startTime, "Max steps exceeded");
        } catch (BaseException e) {
            return failRun(runDO, context, events, e.getMessage(), startTime, e.getClass().getSimpleName());
        } catch (Exception e) {
            log.error("Agent execution failed: runId={}, agentId={}", runDO.getId(), agentId, e);
            return failRun(runDO, context, events, "System error: " + e.getMessage(),
                    startTime, e.getClass().getSimpleName());
        }
    }

    public AgentRunResult getRunStatus(Long runId) {
        AgentRunDO runDO = agentRunMapper.selectById(runId);
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
        AgentDO agentDO = agentMapper.selectById(agentId);
        if (agentDO == null) {
            throw new BaseException(ErrorCodeEnum.AGENT_NOT_EXIST);
        }

        AgentRunDO runDO = createRunRecord(agentId, task);
        runDO.setStatus("EXECUTING");
        agentRunMapper.updateById(runDO);

        List<AgentRunEvent> events = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        try (AgentRunEventCollector collector = AgentRunEventCollector.open()) {
            collector.add(event(AgentRunEvent.EventType.REASONING, null, null,
                    Map.of("strategy", ExecutionStrategy.AI_SERVICES.name()), true, null,
                    "AiServices execution started"));

            AgentAiService aiService = AiServices
                    .builder(AgentAiService.class)
                    .chatLanguageModel(dynamicModelManager.getChatModel(ModelTypeEnum.CHAT))
                    .systemMessageProvider(memoryId ->
                            agentDO.getSystemPrompt() != null && !agentDO.getSystemPrompt().isBlank()
                                    ? agentDO.getSystemPrompt()
                                    : "You are an intelligent assistant. Use tools when needed to complete the task.")
                    .toolProvider(dynamicToolProvider)
                    .chatMemory(memoryFactory.createDefault())
                    .build();

            String answer = aiService.chat(task);
            events.addAll(collector.events());
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

    private AgentRunResult completeRun(AgentRunDO runDO, AgentContext context,
                                       List<AgentRunEvent> events, String answer, long startTime) {
        context.setStatus(AgentContext.AgentContextStatus.COMPLETED);
        events.add(event(AgentRunEvent.EventType.FINAL_ANSWER, null, null,
                Map.of("answer", answer), true, null, "Final answer"));
        updateRunRecord(runDO, "COMPLETED", answer, events);
        long totalDuration = System.currentTimeMillis() - startTime;
        return buildResult(runDO.getId(), answer, "COMPLETED", context.getSteps(), events, totalDuration);
    }

    private AgentRunResult failRun(AgentRunDO runDO, AgentContext context,
                                   List<AgentRunEvent> events, String message,
                                   long startTime, String reason) {
        context.setStatus(AgentContext.AgentContextStatus.FAILED);
        events.add(event(AgentRunEvent.EventType.ERROR, null, null,
                Map.of("reason", reason), false, null, message));
        updateRunRecord(runDO, "FAILED", message, events);
        long totalDuration = System.currentTimeMillis() - startTime;
        return buildResult(runDO.getId(), message, "FAILED", context.getSteps(), events, totalDuration);
    }

    private void executeAndRecord(AgentContext context, PlanStep step, List<AgentRunEvent> events) {
        events.add(event(AgentRunEvent.EventType.TOOL_CALL, step.stepOrder(), step.toolName(),
                Map.of("parameters", step.parameters() != null ? step.parameters() : Map.of(),
                        "reasoning", step.reasoning() != null ? step.reasoning() : ""),
                null, null, "Plan-Execute tool call"));

        Observation observation = agentExecutor.executeStep(step);
        events.add(event(AgentRunEvent.EventType.OBSERVATION, observation.stepOrder(), observation.toolName(),
                Map.of("result", observation.result() != null ? observation.result() : "",
                        "errorMessage", observation.errorMessage() != null ? observation.errorMessage() : ""),
                observation.success(), observation.durationMs(), "Tool observation"));

        context.addStep(new AgentStep(
                step.stepOrder(), step.toolName(), step.parameters(),
                observation.result(), observation.durationMs(),
                observation.success(), observation.errorMessage()
        ));
    }

    private AgentRunDO createRunRecord(Long agentId, String task) {
        AgentRunDO runDO = new AgentRunDO();
        runDO.setAgentId(agentId);
        runDO.setInputQuery(task);
        runDO.setStatus("PLANNING");
        runDO.setStartTime(LocalDateTime.now());
        runDO.setCreateTime(LocalDateTime.now());
        agentRunMapper.insert(runDO);
        return runDO;
    }

    private void updateRunRecord(AgentRunDO runDO, String status, String output, List<AgentRunEvent> events) {
        runDO.setStatus(status);
        runDO.setOutputAnswer(output);
        runDO.setEndTime(LocalDateTime.now());
        runDO.setLog(serializeEvents(events));
        agentRunMapper.updateById(runDO);
    }

    private void recordReasoning(List<AgentRunEvent> events, ReasoningResult reasoning, String message) {
        events.add(event(AgentRunEvent.EventType.REASONING, null, null,
                Map.of("decision", reasoning.decision().name(), "content", reasoning.content()),
                true, null, message));
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
        try {
            List<AgentStep> legacySteps = objectMapper.readValue(logJson, new TypeReference<List<AgentStep>>() {});
            return stepsToEvents(legacySteps);
        } catch (Exception e) {
            log.error("Failed to parse agent run log", e);
            return List.of();
        }
    }

    private List<AgentStep> eventsToSteps(List<AgentRunEvent> events) {
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

    private AgentResp toAgentResp(AgentDO agentDO, List<Long> toolIds) {
        return new AgentResp(agentDO.getId(), agentDO.getName(),
                agentDO.getDescription(), toolIds != null ? toolIds : List.of(), agentDO.getSystemPrompt(),
                agentDO.getMaxSteps(), agentDO.getCreateTime() != null
                        ? agentDO.getCreateTime().toString() : null);
    }
}
