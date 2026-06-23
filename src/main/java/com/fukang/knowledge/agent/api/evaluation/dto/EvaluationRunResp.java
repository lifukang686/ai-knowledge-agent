package com.fukang.knowledge.agent.api.evaluation.dto;

import java.time.LocalDateTime;

/**
 * 评测运行响应。
 */
public record EvaluationRunResp(
        Long id,
        Long datasetId,
        String name,
        String targetType,
        String status,
        Integer totalCount,
        Integer passedCount,
        Integer failedCount,
        Double avgScore,
        Long avgLatencyMs,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        String errorMessage,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
