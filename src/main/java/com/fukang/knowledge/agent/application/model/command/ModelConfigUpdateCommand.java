package com.fukang.knowledge.agent.application.model.command;

public record ModelConfigUpdateCommand(
        Long providerId,
        String modelName,
        String modelType,
        String defaultParams
) {}
