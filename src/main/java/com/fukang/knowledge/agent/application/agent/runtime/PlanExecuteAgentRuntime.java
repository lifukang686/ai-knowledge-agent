package com.fukang.knowledge.agent.application.agent.runtime;

import com.fukang.knowledge.agent.application.agent.tool.ToolScope;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.domain.agent.model.AgentContext;
import com.fukang.knowledge.agent.domain.agent.model.AgentRunEvent;
import com.fukang.knowledge.agent.domain.agent.model.AgentStep;
import com.fukang.knowledge.agent.domain.agent.model.Observation;
import com.fukang.knowledge.agent.domain.agent.model.PlanStep;
import com.fukang.knowledge.agent.domain.agent.model.ReasoningResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 可复用的 Plan-Execute Agent 运行时。
 * <p>负责“规划 → 推理 → 工具调用 → 观察 → 最终答案”的核心循环。
 * 业务方通过 {@link AgentRuntimeOptions#toolScope()} 限定工具集合，避免 LLM 越权规划。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlanExecuteAgentRuntime {

    /**
     * Agent 规划器。
     */
    private final AgentPlanner agentPlanner;

    /**
     * Agent 执行器。
     */
    private final AgentExecutor agentExecutor;

    /**
     * Agent 推理器。
     */
    private final AgentReasoner agentReasoner;

    /**
     * 执行一次 Plan-Execute Agent 任务。
     *
     * @param task    任务内容
     * @param options 运行配置
     * @return 运行结果
     */
    public RuntimeResult runTask(String task, AgentRuntimeOptions options) {
        AgentRuntimeOptions runtimeOptions = options != null
                ? options
                : AgentRuntimeOptions.of(5, "", null);
        AgentContext context = new AgentContext(task);
        List<AgentRunEvent> events = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        try {
            context.setStatus(AgentContext.AgentContextStatus.PLANNING);
            List<PlanStep> plan = agentPlanner.plan(task, runtimeOptions.toolScope(), runtimeOptions.planningExtraPrompt());
            context.setPlanSteps(plan);
            events.add(event(AgentRunEvent.EventType.PLAN, null, null,
                    Map.of("steps", plan), true, null, "Plan generated"));

            context.setStatus(AgentContext.AgentContextStatus.EXECUTING);
            int stepCount = 0;
            while (stepCount < runtimeOptions.maxSteps()) {
                // 每轮先让 Reasoner 基于历史观察判断：继续、重试、终止或生成最终答案。
                ReasoningResult reasoning = agentReasoner.reason(context);
                recordReasoning(events, reasoning, "Reasoning decision");

                if (reasoning.decision() == ReasoningResult.Decision.FINAL_ANSWER) {
                    return complete(context, events, reasoning.content(), startTime);
                }
                if (reasoning.decision() == ReasoningResult.Decision.ABORT) {
                    return fail(context, events, reasoning.content(), startTime, "Agent aborted");
                }
                if (reasoning.decision() == ReasoningResult.Decision.RETRY) {
                    AgentStep lastStep = context.getLastStep();
                    if (lastStep != null) {
                        // 重试复用上一步工具和参数，仅修改说明，避免重新规划引入额外不确定性。
                        PlanStep retryStep = new PlanStep(lastStep.stepOrder(),
                                lastStep.toolName(), lastStep.parameters(), "Retry previous step");
                        executeAndRecord(context, retryStep, runtimeOptions.toolScope(), events);
                        stepCount++;
                    }
                    continue;
                }

                if (context.getRemainingSteps().isEmpty()) {
                    ReasoningResult finalReason = agentReasoner.reason(context);
                    recordReasoning(events, finalReason, "Final reasoning decision");
                    if (finalReason.decision() == ReasoningResult.Decision.FINAL_ANSWER) {
                        return complete(context, events, finalReason.content(), startTime);
                    }
                    break;
                }

                PlanStep nextStep = context.getRemainingSteps().get(0);
                executeAndRecord(context, nextStep, runtimeOptions.toolScope(), events);
                stepCount++;
            }

            return fail(context, events, "Agent reached max steps limit: " + runtimeOptions.maxSteps(),
                    startTime, "Max steps exceeded");
        } catch (BaseException e) {
            return fail(context, events, e.getMessage(), startTime, e.getClass().getSimpleName());
        } catch (Exception e) {
            log.error("Plan-Execute runtime failed", e);
            return fail(context, events, "System error: " + e.getMessage(), startTime,
                    e.getClass().getSimpleName());
        }
    }

    /**
     * 执行步骤并记录事件。
     *
     * @param context   运行上下文
     * @param step      计划步骤
     * @param toolScope 工具范围
     * @param events    事件列表
     */
    private void executeAndRecord(AgentContext context, PlanStep step, ToolScope toolScope,
                                  List<AgentRunEvent> events) {
        events.add(event(AgentRunEvent.EventType.TOOL_CALL, step.stepOrder(), step.toolName(),
                Map.of("parameters", step.parameters() != null ? step.parameters() : Map.of(),
                        "reasoning", step.reasoning() != null ? step.reasoning() : ""),
                null, null, "Plan-Execute tool call"));

        Observation observation = agentExecutor.executeStep(step, toolScope);
        events.add(event(AgentRunEvent.EventType.OBSERVATION, observation.stepOrder(), observation.toolName(),
                Map.of("result", observation.result() != null ? observation.result() : "",
                        "errorMessage", observation.errorMessage() != null ? observation.errorMessage() : ""),
                observation.success(), observation.durationMs(), "Tool observation"));

        context.addStep(new AgentStep(step.stepOrder(), step.toolName(), step.parameters(),
                observation.result(), observation.durationMs(), observation.success(), observation.errorMessage()));
    }

    /**
     * 完成任务并返回结果。
     *
     * @param context   运行上下文
     * @param events    事件列表
     * @param answer    最终回答
     * @param startTime 开始时间
     * @return 运行结果
     */
    private RuntimeResult complete(AgentContext context, List<AgentRunEvent> events,
                                   String answer, long startTime) {
        context.setStatus(AgentContext.AgentContextStatus.COMPLETED);
        events.add(event(AgentRunEvent.EventType.FINAL_ANSWER, null, null,
                Map.of("answer", answer != null ? answer : ""), true, null, "Final answer"));
        return new RuntimeResult(answer, "COMPLETED", context.getSteps(),
                List.copyOf(events), System.currentTimeMillis() - startTime);
    }

    /**
     * 标记失败并返回结果。
     *
     * @param context   运行上下文
     * @param events    事件列表
     * @param message   失败消息
     * @param startTime 开始时间
     * @param reason    失败原因
     * @return 运行结果
     */
    private RuntimeResult fail(AgentContext context, List<AgentRunEvent> events,
                               String message, long startTime, String reason) {
        context.setStatus(AgentContext.AgentContextStatus.FAILED);
        events.add(event(AgentRunEvent.EventType.ERROR, null, null,
                Map.of("reason", reason != null ? reason : ""), false, null, message));
        return new RuntimeResult(message, "FAILED", context.getSteps(),
                List.copyOf(events), System.currentTimeMillis() - startTime);
    }

    /**
     * 记录推理事件。
     *
     * @param events    事件列表
     * @param reasoning 推理结果
     * @param message   展示消息
     */
    private void recordReasoning(List<AgentRunEvent> events, ReasoningResult reasoning, String message) {
        events.add(event(AgentRunEvent.EventType.REASONING, null, null,
                Map.of("decision", reasoning.decision().name(), "content", reasoning.content()),
                true, null, message));
    }

    /**
     * 创建运行事件。
     *
     * @param type       事件类型
     * @param stepOrder  步骤序号
     * @param toolName   工具名称
     * @param payload    事件载荷
     * @param success    是否成功
     * @param durationMs 执行耗时
     * @param message    展示消息
     * @return 运行事件
     */
    private AgentRunEvent event(AgentRunEvent.EventType type, Integer stepOrder, String toolName,
                                Map<String, Object> payload, Boolean success, Long durationMs, String message) {
        return AgentRunEvent.of(type, stepOrder, toolName, payload, success, durationMs, message);
    }

    /**
     * Agent 运行结果。
     */
    public record RuntimeResult(
            /**
             * 最终回答。
             */
            String answer,

            /**
             * 运行状态。
             */
            String status,

            /**
             * 已执行步骤。
             */
            List<AgentStep> steps,

            /**
             * 运行事件列表。
             */
            List<AgentRunEvent> events,

            /**
             * 总耗时。
             */
            long totalDurationMs
    ) {
    }
}
