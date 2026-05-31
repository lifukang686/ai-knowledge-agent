package com.fukang.knowledge.agent.application.knowledge.command;

public record CreateKnowledgeBaseCommand(
        String name,
        String description
) {}
