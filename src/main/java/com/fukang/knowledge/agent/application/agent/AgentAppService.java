package com.fukang.knowledge.agent.application.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.enums.ModelTypeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.domain.agent.model.*;
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
import java.util.List;

/**
 * Agent 应用服务
 * <p>编排完整的 Agent 执行流程，是 Agent 模块的入口服务。
 * 核心流程：规划 → [执行 → 推理 → 循环] → 返回结果
 *
 * <pre>
 * 完整执行流程:
 * 1. 校验 Agent 配置是否存在
 * 2. 创建 agent_run 运行记录
 * 3. 调用 Planner 生成 PlanStep 列表
 * 4. 进入执行-推理循环：
 *    a. 调用 Reasoner 决策下一步动作
 *    b. 如果是 CONTINUE: 获取下一个 PlanStep，调用 Executor 执行
 *    c. 如果是 FINAL_ANSWER: 完成任务，输出结果
 *    d. 如果是 RETRY: 重试上一步
 *    e. 如果是 ABORT: 终止任务
 * 5. 达到最大步数或异常时终止
 * 6. 更新 agent_run 记录，返回 AgentRunResult
 * </pre>
 * </p>
 */
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

    /**
     * 执行 Agent 任务
     *
     * @param agentId  Agent 配置 ID
     * @param task     用户任务描述
     * @param strategy 执行策略
     * @return Agent 运行结果
     * @throws BaseException Agent 配置不存在时抛出
     */
    @Transactional
    public AgentRunResult run(Long agentId, String task, ExecutionStrategy strategy) {
        ExecutionStrategy execStrategy = strategy != null ? strategy : ExecutionStrategy.PLAN_EXECUTE;

        if (execStrategy == ExecutionStrategy.AI_SERVICES) {
            return runWithAiServices(agentId, task);
        }
        return runWithPlanExecute(agentId, task);
    }

    /**
     * 执行 Agent 任务（默认 Plan-Execute 策略）
     */
    @Transactional
    public AgentRunResult run(Long agentId, String task) {
        return run(agentId, task, ExecutionStrategy.PLAN_EXECUTE);
    }

    /**
     * Plan-Then-Execute 策略（已有逻辑不变）
     */
    private AgentRunResult runWithPlanExecute(Long agentId, String task) {
        AgentDO agentDO = agentMapper.selectById(agentId);
        if (agentDO == null) {
            throw new BaseException(ErrorCodeEnum.AGENT_NOT_EXIST);
        }

        int maxSteps = agentDO.getMaxSteps() != null ? agentDO.getMaxSteps()
                : agentProperties.getMaxSteps();

        AgentRunDO runDO = createRunRecord(agentId, task);
        AgentContext context = new AgentContext(task);
        context.setChatMemory(memoryFactory.createDefault());

        long startTime = System.currentTimeMillis();

        try {
            // 1. 规划阶段
            context.setStatus(AgentContext.AgentContextStatus.PLANNING);
            log.info("Agent 开始规划: runId={}, agentId={}, task={}", runDO.getId(), agentId, task);
            List<PlanStep> plan = agentPlanner.plan(task);
            context.setPlanSteps(plan);
            log.info("Agent 规划完成: runId={}, 步骤数={}", runDO.getId(), plan.size());

            // 2. 执行-推理循环
            context.setStatus(AgentContext.AgentContextStatus.EXECUTING);
            int stepCount = 0;

            while (stepCount < maxSteps) {
                ReasoningResult reasoning = agentReasoner.reason(context);

                if (reasoning.decision() == ReasoningResult.Decision.FINAL_ANSWER) {
                    log.info("Agent 任务完成: runId={}, 总步数={}", runDO.getId(), stepCount);
                    context.setStatus(AgentContext.AgentContextStatus.COMPLETED);
                    updateRunRecord(runDO, "COMPLETED", reasoning.content(), context);
                    long totalDuration = System.currentTimeMillis() - startTime;
                    return buildResult(runDO.getId(), reasoning.content(),
                            "COMPLETED", context.getSteps(), totalDuration);
                }

                if (reasoning.decision() == ReasoningResult.Decision.ABORT) {
                    log.warn("Agent 任务终止: runId={}, reason={}", runDO.getId(), reasoning.content());
                    context.setStatus(AgentContext.AgentContextStatus.FAILED);
                    updateRunRecord(runDO, "FAILED", reasoning.content(), context);
                    long totalDuration = System.currentTimeMillis() - startTime;
                    return buildResult(runDO.getId(), reasoning.content(),
                            "FAILED", context.getSteps(), totalDuration);
                }

                if (reasoning.decision() == ReasoningResult.Decision.RETRY) {
                    log.info("Agent 重试上一步: runId={}", runDO.getId());
                    AgentStep lastStep = context.getLastStep();
                    if (lastStep != null) {
                        PlanStep retryStep = new PlanStep(lastStep.stepOrder(),
                                lastStep.toolName(), lastStep.parameters(), "重试");
                        executeAndRecord(context, retryStep);
                        stepCount++;
                    }
                    continue;
                }

                // CONTINUE: 执行下一个计划步骤
                if (context.getRemainingSteps().isEmpty()) {
                    ReasoningResult finalReason = agentReasoner.reason(context);
                    if (finalReason.decision() == ReasoningResult.Decision.FINAL_ANSWER) {
                        context.setStatus(AgentContext.AgentContextStatus.COMPLETED);
                        updateRunRecord(runDO, "COMPLETED", finalReason.content(), context);
                        long totalDuration = System.currentTimeMillis() - startTime;
                        return buildResult(runDO.getId(), finalReason.content(),
                                "COMPLETED", context.getSteps(), totalDuration);
                    }
                    log.warn("无剩余步骤但推理未返回 FINAL_ANSWER: runId={}", runDO.getId());
                    break;
                }

                PlanStep nextStep = context.getRemainingSteps().get(0);
                executeAndRecord(context, nextStep);
                stepCount++;
                log.info("步骤执行完成: runId={}, step={}/{}, tool={}, completedSteps={}",
                        runDO.getId(), stepCount, context.getTotalStepCount(),
                        nextStep.toolName(), context.getCompletedStepCount());
            }

            // 3. 达到最大步数限制
            log.warn("Agent 达到最大步数限制: runId={}, maxSteps={}", runDO.getId(), maxSteps);
            context.setStatus(AgentContext.AgentContextStatus.FAILED);
            String errorMsg = "达到最大执行步数限制 (" + maxSteps + ")，任务未完成";
            updateRunRecord(runDO, "FAILED", errorMsg, context);
            long totalDuration = System.currentTimeMillis() - startTime;
            return buildResult(runDO.getId(), errorMsg, "FAILED",
                    context.getSteps(), totalDuration);

        } catch (BaseException e) {
            log.error("Agent 执行异常: runId={}, agentId={}", runDO.getId(), agentId, e);
            context.setStatus(AgentContext.AgentContextStatus.FAILED);
            updateRunRecord(runDO, "FAILED", e.getMessage(), context);
            return buildResult(runDO.getId(), e.getMessage(), "FAILED",
                    context.getSteps(), System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            log.error("Agent 执行未知异常: runId={}, agentId={}", runDO.getId(), agentId, e);
            context.setStatus(AgentContext.AgentContextStatus.FAILED);
            String errorMsg = "系统异常: " + e.getMessage();
            updateRunRecord(runDO, "FAILED", errorMsg, context);
            return buildResult(runDO.getId(), errorMsg, "FAILED",
                    context.getSteps(), System.currentTimeMillis() - startTime);
        }
    }

    /**
     * 查询 Agent 运行状态
     *
     * @param runId 运行记录 ID
     * @return Agent 运行结果
     */
    public AgentRunResult getRunStatus(Long runId) {
        AgentRunDO runDO = agentRunMapper.selectById(runId);
        if (runDO == null) {
            throw new BaseException(ErrorCodeEnum.AGENT_RUN_NOT_EXIST);
        }
        List<AgentStep> steps = parseSteps(runDO.getLog());
        return buildResult(runId, runDO.getOutputAnswer(), runDO.getStatus(),
                steps, calculateDuration(runDO.getStartTime(), runDO.getEndTime()));
    }

    /**
     * AiServices 轻量执行策略（ReAct 模式）
     * <p>利用 LangChain4j 的 {@link AiServices} 框架，让 LLM 通过 Function Calling
     * 自主决定何时调用哪个工具。框架自动管理 Thought → Action → Observation 循环，
     * 无需手动编写工具调用循环逻辑</p>
     *
     * @param agentId Agent 配置 ID
     * @param task    用户任务描述
     * @return Agent 运行结果
     */
    @Transactional
    public AgentRunResult runWithAiServices(Long agentId, String task) {
        AgentDO agentDO = agentMapper.selectById(agentId);
        if (agentDO == null) {
            throw new BaseException(ErrorCodeEnum.AGENT_NOT_EXIST);
        }

        AgentRunDO runDO = createRunRecord(agentId, task);
        runDO.setStatus("EXECUTING");
        agentRunMapper.updateById(runDO);

        log.info("Agent [AI_SERVICES] 开始执行: runId={}, agentId={}, task={}", runDO.getId(), agentId, task);

        long startTime = System.currentTimeMillis();

        try {
            AgentAiService aiService = AiServices
                    .builder(AgentAiService.class)
                    .chatLanguageModel(dynamicModelManager.getChatModel(ModelTypeEnum.CHAT))
                    .systemMessageProvider(memoryId ->
                            agentDO.getSystemPrompt() != null && !agentDO.getSystemPrompt().isBlank()
                                    ? agentDO.getSystemPrompt()
                                    : "你是一个智能助手，能够根据需要调用工具来帮助用户完成任务。")
                    .toolProvider(dynamicToolProvider)
                    .chatMemory(memoryFactory.createDefault())
                    .build();

            String answer = aiService.chat(task);

            long totalDuration = System.currentTimeMillis() - startTime;
            AgentContext context = new AgentContext(task);
            context.setStatus(AgentContext.AgentContextStatus.COMPLETED);
            updateRunRecord(runDO, "COMPLETED", answer, context);

            log.info("Agent [AI_SERVICES] 执行完成: runId={}, duration={}ms", runDO.getId(), totalDuration);
            return buildResult(runDO.getId(), answer, "COMPLETED", List.of(), totalDuration);

        } catch (Exception e) {
            log.error("Agent [AI_SERVICES] 执行异常: runId={}, agentId={}", runDO.getId(), agentId, e);
            long totalDuration = System.currentTimeMillis() - startTime;
            String errorMsg = "执行异常: " + e.getMessage();
            AgentContext context = new AgentContext(task);
            context.setStatus(AgentContext.AgentContextStatus.FAILED);
            updateRunRecord(runDO, "FAILED", errorMsg, context);
            return buildResult(runDO.getId(), errorMsg, "FAILED", List.of(), totalDuration);
        }
    }

    private void executeAndRecord(AgentContext context, PlanStep step) {
        Observation observation = agentExecutor.executeStep(step);
        AgentStep agentStep = new AgentStep(
                step.stepOrder(), step.toolName(), step.parameters(),
                observation.result(), observation.durationMs(),
                observation.success(), observation.errorMessage()
        );
        context.addStep(agentStep);
    }

    private AgentRunDO createRunRecord(Long agentId, String task) {
        AgentRunDO runDO = new AgentRunDO();
        runDO.setAgentId(agentId);
        runDO.setInputQuery(task);
        runDO.setStatus("PLANNING");
        runDO.setStartTime(LocalDateTime.now());
        runDO.setCreateTime(LocalDateTime.now());
        agentRunMapper.insert(runDO);
        log.info("创建 Agent 运行记录: runId={}, agentId={}", runDO.getId(), agentId);
        return runDO;
    }

    private void updateRunRecord(AgentRunDO runDO, String status, String output,
                                  AgentContext context) {
        runDO.setStatus(status);
        runDO.setOutputAnswer(output);
        runDO.setEndTime(LocalDateTime.now());
        runDO.setLog(serializeSteps(context.getSteps()));
        agentRunMapper.updateById(runDO);
    }

    private String serializeSteps(List<AgentStep> steps) {
        try {
            return objectMapper.writeValueAsString(steps);
        } catch (Exception e) {
            log.error("序列化步骤日志失败", e);
            return "[]";
        }
    }

    private List<AgentStep> parseSteps(String logJson) {
        if (logJson == null || logJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(logJson, new TypeReference<List<AgentStep>>() {});
        } catch (Exception e) {
            log.error("解析步骤日志失败", e);
            return List.of();
        }
    }

    private long calculateDuration(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        return Duration.between(start, end).toMillis();
    }

    private AgentRunResult buildResult(Long runId, String result, String status,
                                        List<AgentStep> steps, long totalDuration) {
        List<AgentStepRecord> stepRecords = steps.stream()
                .map(s -> new AgentStepRecord(
                        s.stepOrder(),
                        s.toolName(),
                        s.observation() != null ? s.observation() : "",
                        Boolean.TRUE.equals(s.success()),
                        s.durationMs() != null ? s.durationMs() : 0
                ))
                .toList();
        return new AgentRunResult(runId, result, status, stepRecords, totalDuration);
    }
}