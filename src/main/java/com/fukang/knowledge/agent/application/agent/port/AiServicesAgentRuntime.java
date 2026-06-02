package com.fukang.knowledge.agent.application.agent.port;

import com.fukang.knowledge.agent.domain.agent.model.AgentRunEvent;

import java.util.List;

/**
 * LangChain4j AiServices Agent 运行端口。
 */
public interface AiServicesAgentRuntime {

    /**
     * 执行任务并返回最终答案及工具调用事件。
     */
    ExecutionResult execute(String task, String systemPrompt);

    /**
     * AiServices 执行结果。
     */
    record ExecutionResult(
            String answer,
            List<AgentRunEvent> events
    ) {}
}
