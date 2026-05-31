package com.fukang.knowledge.agent.application.agent.command;

import java.util.List;

public record AgentCreateCommand(
        String name,
        String description,
        List<Long> toolIds,
        String systemPrompt,
        Integer maxSteps
) {}
