package com.fukang.knowledge.agent.application.knowledge.command;

/**
 * 创建知识库命令。
 */
public record CreateKnowledgeBaseCommand(
        String name,
        String description
) {}
