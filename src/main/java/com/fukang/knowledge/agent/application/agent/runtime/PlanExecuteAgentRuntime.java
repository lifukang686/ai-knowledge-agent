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
        AgentRuntimeOptions runtimeOptions = options != null ? options : AgentRuntimeOptions.of(5, "", null);
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

    private void plan(String task, AgentRuntimeOptions options, AgentContext context, List<AgentRunEvent> events) {
        context.setStatus(AgentContext.AgentContextStatus.PLANNING);
        List<PlanStep> plan = agentPlanner.plan(task, options.toolScope(), options.planningExtraPrompt());
        context.setPlanSteps(plan);
        recordEvent(events, options, event(AgentRunEvent.EventType.PLAN, null, null,
                Map.of("steps", plan), true, null, "规划已生成"));
        context.setStatus(AgentContext.AgentContextStatus.EXECUTING);
    }

    private RuntimeResult executeLoop(AgentContext context, AgentRuntimeOptions options,
                                      List<AgentRunEvent> events, long startTime) {
        int stepCount = 0;
        while (stepCount < options.maxSteps()) {
            ReasoningResult reasoning = agentReasoner.reason(context);
            recordReasoning(events, options, reasoning, "推理决策");

            RuntimeResult terminal = tryTerminalDecision(context, options, events, reasoning, startTime);
            if (terminal != null) {
                return terminal;
            }
            if (reasoning.decision() == ReasoningResult.Decision.RETRY) {
                stepCount += retryLastStep(context, options, events);
                continue;
            }
            if (context.getRemainingSteps().isEmpty()) {
                RuntimeResult finalResult = tryFinalReasoning(context, options, events, startTime);
                if (finalResult != null) {
                    return finalResult;
                }
                break;
            }

            executeAndRecord(context, context.getRemainingSteps().get(0), options, events);
            stepCount++;
        }
        return fail(context, options, events, "智能体达到最大执行步数限制：" + options.maxSteps(),
                startTime, "超过最大执行步数");
    }

    private RuntimeResult tryTerminalDecision(AgentContext context, AgentRuntimeOptions options,
                                              List<AgentRunEvent> events, ReasoningResult reasoning,
                                              long startTime) {
        if (reasoning.decision() == ReasoningResult.Decision.FINAL_ANSWER) {
            return complete(context, options, events, reasoning.content(), startTime);
        }
        if (reasoning.decision() == ReasoningResult.Decision.ABORT) {
            return fail(context, options, events, reasoning.content(), startTime, "智能体已终止");
        }
        return null;
    }

    private int retryLastStep(AgentContext context, AgentRuntimeOptions options, List<AgentRunEvent> events) {
        AgentStep lastStep = context.getLastStep();
        if (lastStep == null) {
            return 0;
        }
        PlanStep retryStep = new PlanStep(lastStep.stepOrder(), lastStep.toolName(),
                lastStep.parameters(), "重试上一步");
        executeAndRecord(context, retryStep, options, events);
        return 1;
    }

    private RuntimeResult tryFinalReasoning(AgentContext context, AgentRuntimeOptions options,
                                            List<AgentRunEvent> events, long startTime) {
        ReasoningResult finalReason = agentReasoner.reason(context);
        recordReasoning(events, options, finalReason, "最终推理决策");
        if (finalReason.decision() == ReasoningResult.Decision.FINAL_ANSWER) {
            return complete(context, options, events, finalReason.content(), startTime);
        }
        return null;
    }

    private void executeAndRecord(AgentContext context, PlanStep step, AgentRuntimeOptions options,
                                  List<AgentRunEvent> events) {
        ToolScope toolScope = options.toolScope();
        recordEvent(events, options, event(AgentRunEvent.EventType.TOOL_CALL, step.stepOrder(), step.toolName(),
                Map.of("parameters", step.parameters() != null ? step.parameters() : Map.of(),
                        "reasoning", step.reasoning() != null ? step.reasoning() : ""),
                null, null, "规划执行工具调用"));

        Observation observation = agentExecutor.executeStep(step, toolScope);
        recordEvent(events, options, event(AgentRunEvent.EventType.OBSERVATION,
                observation.stepOrder(), observation.toolName(),
                Map.of("result", observation.result() != null ? observation.result() : "",
                        "errorMessage", observation.errorMessage() != null ? observation.errorMessage() : ""),
                observation.success(), observation.durationMs(), "工具观察结果"));

        context.addStep(new AgentStep(step.stepOrder(), step.toolName(), step.parameters(),
                observation.result(), observation.durationMs(), observation.success(), observation.errorMessage()));
    }

    private RuntimeResult complete(AgentContext context, AgentRuntimeOptions options, List<AgentRunEvent> events,
                                   String answer, long startTime) {
        context.setStatus(AgentContext.AgentContextStatus.COMPLETED);
        recordEvent(events, options, event(AgentRunEvent.EventType.FINAL_ANSWER, null, null,
                Map.of("answer", answer != null ? answer : ""), true, null, "最终答案"));
        return new RuntimeResult(answer, "COMPLETED", context.getSteps(),
                List.copyOf(events), System.currentTimeMillis() - startTime);
    }

    private RuntimeResult fail(AgentContext context, AgentRuntimeOptions options, List<AgentRunEvent> events,
                               String message, long startTime, String reason) {
        context.setStatus(AgentContext.AgentContextStatus.FAILED);
        recordEvent(events, options, event(AgentRunEvent.EventType.ERROR, null, null,
                Map.of("reason", reason != null ? reason : ""), false, null, message));
        return new RuntimeResult(message, "FAILED", context.getSteps(),
                List.copyOf(events), System.currentTimeMillis() - startTime);
    }

    private void recordReasoning(List<AgentRunEvent> events, AgentRuntimeOptions options,
                                 ReasoningResult reasoning, String message) {
        recordEvent(events, options, event(AgentRunEvent.EventType.REASONING, null, null,
                Map.of("decision", reasoning.decision().name(), "content", reasoning.content()),
                true, null, message));
    }

    private void recordEvent(List<AgentRunEvent> events, AgentRuntimeOptions options, AgentRunEvent event) {
        events.add(event);
        if (options.eventListener() != null) {
            options.eventListener().accept(event);
        }
    }

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
