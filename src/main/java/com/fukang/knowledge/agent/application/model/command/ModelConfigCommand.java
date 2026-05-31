package com.fukang.knowledge.agent.application.model.command;

public record ModelConfigCommand(
        Long providerId,
        String modelName,
        String modelType,
        String defaultParams
) {}
