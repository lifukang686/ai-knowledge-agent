package com.fukang.knowledge.agent.module.agent.service.runtime;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fukang.knowledge.agent.module.agent.service.tool.ToolScope;
import com.fukang.knowledge.agent.module.agent.model.vo.AgentRunEvent;

import java.util.function.Consumer;

/**
 * Plan-Execute 单次运行配置。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentRuntimeOptions {
    /** 最大工具执行步数 */
    private int maxSteps;

    /** 规划阶段额外业务约束 */
    private String planningExtraPrompt;

    /** 本次运行可见工具集合 */
    private ToolScope toolScope;

    /** 运行事件监听器，可用于 SSE 推送或外部审计 */
    private Consumer<AgentRunEvent> eventListener;

    public static AgentRuntimeOptions of(int maxSteps, String planningExtraPrompt, ToolScope toolScope) {
        return new AgentRuntimeOptions(maxSteps, planningExtraPrompt, toolScope, null);
    }

    public AgentRuntimeOptions withEventListener(Consumer<AgentRunEvent> listener) {
        return new AgentRuntimeOptions(maxSteps, planningExtraPrompt, toolScope, listener);
    }

}
