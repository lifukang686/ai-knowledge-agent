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
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlanExecuteAgentRuntime {

    private final AgentPlanner agentPlanner;
    private final AgentExecutor agentExecutor;
    private final AgentReasoner agentReasoner;

    /**
     * 执行一次 Plan-Execute Agent 任务。
     */
    public RuntimeResult runTask(String task, AgentRuntimeOptions options) {
        // 未传配置时使用保守默认值，保证 Runtime 可独立复用。
        AgentRuntimeOptions runtimeOptions = options != null ? options : AgentRuntimeOptions.of(5, "", null);
        // AgentContext 保存计划、已执行步骤和当前状态，是整轮运行的内存态。
        AgentContext context = new AgentContext(task);
        List<AgentRunEvent> events = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        try {
            plan(task, runtimeOptions, context, events);
            return executeLoop(context, runtimeOptions, events, startTime);
        } catch (BaseException e) {
            return fail(context, runtimeOptions, events, e.getMessage(), startTime, e.getClass().getSimpleName());
        } catch (Exception e) {
            log.error("规划执行运行时执行失败", e);
            return fail(context, runtimeOptions, events, "系统错误：" + e.getMessage(), startTime,
                    e.getClass().getSimpleName());
        }
    }

    /**
     * 生成执行计划并记录规划事件。
     */
    private void plan(String task, AgentRuntimeOptions options, AgentContext context, List<AgentRunEvent> events) {
        context.setStatus(AgentContext.AgentContextStatus.PLANNING);
        // Planner 基于任务、工具范围和额外提示词生成可执行步骤。
        List<PlanStep> plan = agentPlanner.plan(task, options.toolScope(), options.planningExtraPrompt());
        context.setPlanSteps(plan);
        recordEvent(events, options, event(AgentRunEvent.EventType.PLAN, null, null,
                Map.of("steps", plan), true, null, "规划已生成"));
        // 规划完成后进入执行态，后续由 Reasoner 决定每轮动作。
        context.setStatus(AgentContext.AgentContextStatus.EXECUTING);
    }

    /**
     * 按计划循环执行工具，并根据推理结果决定收敛、重试或失败。
     */
    private RuntimeResult executeLoop(AgentContext context, AgentRuntimeOptions options,
                                      List<AgentRunEvent> events, long startTime) {
        int stepCount = 0;
        while (stepCount < options.maxSteps()) {
            // 每轮先让 Reasoner 根据上下文判断继续、重试、终止还是直接回答。
            ReasoningResult reasoning = agentReasoner.reason(context);
            recordReasoning(events, options, reasoning, "推理决策");

            RuntimeResult terminal = tryTerminalDecision(context, options, events, reasoning, startTime);
            if (terminal != null) {
                return terminal;
            }
            if (reasoning.decision() == ReasoningResult.Decision.RETRY) {
                // 重试只重复最近一次工具调用，并计入执行步数。
                stepCount += retryLastStep(context, options, events);
                continue;
            }
            if (context.getRemainingSteps().isEmpty()) {
                // 计划步骤执行完后再推理一次，争取生成最终答案。
                RuntimeResult finalResult = tryFinalReasoning(context, options, events, startTime);
                if (finalResult != null) {
                    return finalResult;
                }
                break;
            }

            // 默认执行下一条未完成计划步骤。
            executeAndRecord(context, context.getRemainingSteps().get(0), options, events);
            stepCount++;
        }
        return fail(context, options, events, "智能体达到最大执行步数限制：" + options.maxSteps(),
                startTime, "超过最大执行步数");
    }

    /**
     * 处理可立即结束运行的推理决策。
     */
    private RuntimeResult tryTerminalDecision(AgentContext context, AgentRuntimeOptions options,
                                              List<AgentRunEvent> events, ReasoningResult reasoning,
                                              long startTime) {
        if (reasoning.decision() == ReasoningResult.Decision.FINAL_ANSWER) {
            // Reasoner 已能给出答案时，直接收敛。
            return complete(context, options, events, reasoning.content(), startTime);
        }
        if (reasoning.decision() == ReasoningResult.Decision.ABORT) {
            // 明确终止时按失败收尾，并保留终止原因。
            return fail(context, options, events, reasoning.content(), startTime, "智能体已终止");
        }
        return null;
    }

    /**
     * 重试最近一次工具调用。
     */
    private int retryLastStep(AgentContext context, AgentRuntimeOptions options, List<AgentRunEvent> events) {
        AgentStep lastStep = context.getLastStep();
        if (lastStep == null) {
            return 0;
        }
        // 复用上一轮工具名和参数，避免 Reasoner 临时生成越界调用。
        PlanStep retryStep = new PlanStep(lastStep.stepOrder(), lastStep.toolName(),
                lastStep.parameters(), "重试上一步");
        executeAndRecord(context, retryStep, options, events);
        return 1;
    }

    /**
     * 计划执行完后再尝试生成最终答案。
     */
    private RuntimeResult tryFinalReasoning(AgentContext context, AgentRuntimeOptions options,
                                            List<AgentRunEvent> events, long startTime) {
        ReasoningResult finalReason = agentReasoner.reason(context);
        recordReasoning(events, options, finalReason, "最终推理决策");
        if (finalReason.decision() == ReasoningResult.Decision.FINAL_ANSWER) {
            return complete(context, options, events, finalReason.content(), startTime);
        }
        return null;
    }

    /**
     * 执行单个计划步骤并记录工具调用、观察结果。
     */
    private void executeAndRecord(AgentContext context, PlanStep step, AgentRuntimeOptions options,
                                  List<AgentRunEvent> events) {
        ToolScope toolScope = options.toolScope();
        // 先记录工具调用事件，前端可即时展示将要执行的动作。
        recordEvent(events, options, event(AgentRunEvent.EventType.TOOL_CALL, step.stepOrder(), step.toolName(),
                Map.of("parameters", step.parameters() != null ? step.parameters() : Map.of(),
                        "reasoning", step.reasoning() != null ? step.reasoning() : ""),
                null, null, "规划执行工具调用"));

        // Executor 真正调用工具，并把结果包装成 Observation。
        Observation observation = agentExecutor.executeStep(step, toolScope);
        recordEvent(events, options, event(AgentRunEvent.EventType.OBSERVATION,
                observation.stepOrder(), observation.toolName(),
                Map.of("result", observation.result() != null ? observation.result() : "",
                        "errorMessage", observation.errorMessage() != null ? observation.errorMessage() : ""),
                observation.success(), observation.durationMs(), "工具观察结果"));

        // Observation 写回上下文，供下一轮 Reasoner 继续判断。
        context.addStep(new AgentStep(step.stepOrder(), step.toolName(), step.parameters(),
                observation.result(), observation.durationMs(), observation.success(), observation.errorMessage()));
    }

    /**
     * 标记运行成功并返回最终结果。
     */
    private RuntimeResult complete(AgentContext context, AgentRuntimeOptions options, List<AgentRunEvent> events,
                                   String answer, long startTime) {
        context.setStatus(AgentContext.AgentContextStatus.COMPLETED);
        // 成功收尾时补最终答案事件，调用方可直接持久化完整轨迹。
        recordEvent(events, options, event(AgentRunEvent.EventType.FINAL_ANSWER, null, null,
                Map.of("answer", answer != null ? answer : ""), true, null, "最终答案"));
        return new RuntimeResult(answer, "COMPLETED", context.getSteps(),
                List.copyOf(events), System.currentTimeMillis() - startTime);
    }

    /**
     * 标记运行失败并返回失败结果。
     */
    private RuntimeResult fail(AgentContext context, AgentRuntimeOptions options, List<AgentRunEvent> events,
                               String message, long startTime, String reason) {
        context.setStatus(AgentContext.AgentContextStatus.FAILED);
        // 失败也走统一事件格式，便于调用方展示和审计。
        recordEvent(events, options, event(AgentRunEvent.EventType.ERROR, null, null,
                Map.of("reason", reason != null ? reason : ""), false, null, message));
        return new RuntimeResult(message, "FAILED", context.getSteps(),
                List.copyOf(events), System.currentTimeMillis() - startTime);
    }

    /**
     * 记录一次推理决策事件。
     */
    private void recordReasoning(List<AgentRunEvent> events, AgentRuntimeOptions options,
                                 ReasoningResult reasoning, String message) {
        recordEvent(events, options, event(AgentRunEvent.EventType.REASONING, null, null,
                Map.of("decision", reasoning.decision().name(), "content", reasoning.content()),
                true, null, message));
    }

    /**
     * 记录运行事件并通知监听器。
     */
    private void recordEvent(List<AgentRunEvent> events, AgentRuntimeOptions options, AgentRunEvent event) {
        events.add(event);
        if (options.eventListener() != null) {
            // 事件先入内存列表，再同步通知外部监听器。
            options.eventListener().accept(event);
        }
    }

    /**
     * 构造统一的 Agent 运行事件。
     */
    private AgentRunEvent event(AgentRunEvent.EventType type, Integer stepOrder, String toolName,
                                Map<String, Object> payload, Boolean success, Long durationMs, String message) {
        return AgentRunEvent.of(type, stepOrder, toolName, payload, success, durationMs, message);
    }

    /**
     * Agent 运行结果。
     */
    public record RuntimeResult(
            String answer,
            String status,
            List<AgentStep> steps,
            List<AgentRunEvent> events,
            long totalDurationMs
    ) {
    }
}
