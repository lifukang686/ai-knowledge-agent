package com.fukang.knowledge.agent.module.model.application.command;

/**
 * 创建模型提供商命令。
 */
public record ProviderCommand(
        String name,
        String apiBaseUrl,
        String apiKey,
        String description
) {}
