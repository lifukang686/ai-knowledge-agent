package com.fukang.knowledge.agent.application.model.command;

public record ProviderUpdateCommand(
        String name,
        String apiBaseUrl,
        String apiKey,
        String description
) {}
