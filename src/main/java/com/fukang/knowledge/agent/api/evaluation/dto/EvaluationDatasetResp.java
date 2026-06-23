package com.fukang.knowledge.agent.api.evaluation.dto;

import java.time.LocalDateTime;

/**
 * 评测集响应。
 */
public record EvaluationDatasetResp(
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
