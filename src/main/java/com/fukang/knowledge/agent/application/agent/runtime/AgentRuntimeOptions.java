package com.fukang.knowledge.agent.application.agent.runtime;

import com.fukang.knowledge.agent.application.agent.tool.ToolScope;
import com.fukang.knowledge.agent.domain.agent.model.AgentRunEvent;

import java.util.function.Consumer;

/**
 * Plan-Execute 单次运行配置。
 *
 * @param maxSteps               最大工具执行步数
 * @param planningExtraPrompt    规划阶段额外业务约束
 * @param toolScope              本次运行可见工具集合
 * @param persistIntermediateRun 是否由调用方自行持久化运行记录
 */
public record AgentRuntimeOptions(
        int maxSteps,
        String planningExtraPrompt,
        ToolScope toolScope,
        boolean persistIntermediateRun,
        Consumer<AgentRunEvent> eventListener
) {
    public static AgentRuntimeOptions of(int maxSteps, String planningExtraPrompt, ToolScope toolScope) {
        return new AgentRuntimeOptions(maxSteps, planningExtraPrompt, toolScope, false, null);
    }

    public AgentRuntimeOptions withEventListener(Consumer<AgentRunEvent> listener) {
        return new AgentRuntimeOptions(maxSteps, planningExtraPrompt, toolScope, persistIntermediateRun, listener);
    }
}
