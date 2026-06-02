package com.fukang.knowledge.agent.application.model.command;

/**
 * 更新模型配置命令，空字段表示不更新。
 */
public record ModelConfigUpdateCommand(
        Long providerId,
        String modelName,
        String modelType,
        String defaultParams
) {}
