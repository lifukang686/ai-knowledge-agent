package com.fukang.knowledge.agent.application.model.command;

public record ProviderCommand(
        String name,
        String apiBaseUrl,
        String apiKey,
        String description
) {}
