package com.fukang.knowledge.agent.application.model.command;

/**
 * 创建模型配置命令。
 */
public record ModelConfigCommand(
        Long providerId,
        String modelName,
        String modelType,
        String defaultParams
) {}
