package com.fukang.knowledge.agent.api.evaluation.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评测用例响应。
 */
public record EvaluationCaseResp(
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
