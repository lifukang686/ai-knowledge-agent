package com.fukang.knowledge.agent.module.model.application.command;

/**
 * 更新模型提供商命令，空字段表示不更新。
 */
public record ProviderUpdateCommand(
        String name,
        String apiBaseUrl,
        String apiKey,
        String description
) {}
