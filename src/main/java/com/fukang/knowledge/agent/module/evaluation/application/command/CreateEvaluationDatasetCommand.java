package com.fukang.knowledge.agent.module.evaluation.application.command;

/**
 * 创建评测集命令。
 */
public record CreateEvaluationDatasetCommand(
        String name,
        String description,
        Long knowledgeBaseId
) {
}
