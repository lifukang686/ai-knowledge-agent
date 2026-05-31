package com.fukang.knowledge.agent.application.knowledge.command;

public record UpdateKnowledgeBaseCommand(
        String name,
        String description
) {}
