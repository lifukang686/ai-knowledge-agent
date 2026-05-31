package com.fukang.knowledge.agent.application.agent.port;

import com.fukang.knowledge.agent.domain.agent.model.AgentRunEvent;

import java.util.List;

public interface AiServicesAgentRuntime {

    ExecutionResult execute(String task, String systemPrompt);

    record ExecutionResult(
            String answer,
            List<AgentRunEvent> events
    ) {}
}
