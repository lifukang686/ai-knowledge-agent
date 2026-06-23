package com.fukang.knowledge.agent.application.evaluation.result;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评测用例结果。
 */
public record EvaluationCaseResult(
        Long id,
        Long datasetId,
        String question,
        String expectedAnswer,
        List<String> expectedKeywords,
        List<Long> expectedChunkIds,
        String expectedStatus,
        String metadata,
        Boolean enabled,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
