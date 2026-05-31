package com.fukang.knowledge.agent.application.agent.result;

import java.util.List;

public record AgentConfigResult(
        Long id,
        String name,
        String description,
        List<Long> toolIds,
        String systemPrompt,
        Integer maxSteps,
        String createTime
) {}
