package com.fukang.knowledge.agent.application.evaluation.command;

/**
 * 更新评测集命令。
 */
public record UpdateEvaluationDatasetCommand(
        String name,
        String description,
        Long knowledgeBaseId
) {
}
