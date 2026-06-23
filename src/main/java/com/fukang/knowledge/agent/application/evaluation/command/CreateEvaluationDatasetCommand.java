package com.fukang.knowledge.agent.application.evaluation.command;

/**
 * 创建评测集命令。
 */
public record CreateEvaluationDatasetCommand(
        String name,
        String description,
        Long knowledgeBaseId
) {
}
