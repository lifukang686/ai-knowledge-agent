package com.fukang.knowledge.agent.application.knowledge.command;

/**
 * 更新知识库命令，空字段表示保持原值。
 */
public record UpdateKnowledgeBaseCommand(
        String name,
        String description
) {}
