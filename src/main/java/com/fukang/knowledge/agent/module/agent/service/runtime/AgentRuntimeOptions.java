package com.fukang.knowledge.agent.module.agent.service.runtime;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fukang.knowledge.agent.module.agent.service.tool.ToolScope;
import com.fukang.knowledge.agent.module.agent.model.vo.AgentRunEvent;

import java.util.function.Consumer;

/**
 * Plan-Execute 单次运行配置。
 *
 * @param maxSteps               最大工具执行步数
 * @param planningExtraPrompt    规划阶段额外业务约束
 * @param toolScope              本次运行可见工具集合
 * @param eventListener       运行事件监听器，可用于 SSE 推送或外部审计
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentRuntimeOptions {
    private int maxSteps;

    private String planningExtraPrompt;

    private ToolScope toolScope;

    private Consumer<AgentRunEvent> eventListener;

    public static AgentRuntimeOptions of(int maxSteps, String planningExtraPrompt, ToolScope toolScope) {
        return new AgentRuntimeOptions(maxSteps, planningExtraPrompt, toolScope, null);
    }

    public AgentRuntimeOptions withEventListener(Consumer<AgentRunEvent> listener) {
        return new AgentRuntimeOptions(maxSteps, planningExtraPrompt, toolScope, listener);
    }

}
