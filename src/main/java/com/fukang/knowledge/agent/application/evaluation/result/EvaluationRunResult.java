package com.fukang.knowledge.agent.application.evaluation.result;

import java.time.LocalDateTime;

/**
 * 评测运行结果。
 */
public record EvaluationRunResult(
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
