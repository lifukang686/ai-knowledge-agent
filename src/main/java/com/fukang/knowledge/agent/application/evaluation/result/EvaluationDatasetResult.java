package com.fukang.knowledge.agent.application.evaluation.result;

import java.time.LocalDateTime;

/**
 * 评测集结果。
 */
public record EvaluationDatasetResult(
        Long id,
        String name,
        String description,
        Long knowledgeBaseId,
        String targetType,
        long caseCount,
        Long lastRunId,
        String lastRunStatus,
        Double lastAvgScore,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
