package com.fukang.knowledge.agent.application.agent.command;

import java.util.List;

/**
 * 创建 Agent 配置的应用层命令。
 */
public record AgentCreateCommand(
        String name,
        String description,
        List<Long> toolIds,
        String systemPrompt,
        Integer maxSteps
) {}
