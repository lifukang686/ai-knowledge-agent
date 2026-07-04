package com.fukang.knowledge.agent.module.knowledge.application.command;

/**
 * 创建知识库命令。
 */
public record CreateKnowledgeBaseCommand(
        String name,
        String description
) {}
