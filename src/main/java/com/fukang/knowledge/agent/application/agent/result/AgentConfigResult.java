package com.fukang.knowledge.agent.application.agent.result;

import java.util.List;

/**
 * Agent 配置查询结果。
 */
public record AgentConfigResult(
        Long id,
        String name,
        String description,
        List<Long> toolIds,
        String systemPrompt,
        Integer maxSteps,
        String createTime
) {}
