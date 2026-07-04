package com.fukang.knowledge.agent.module.evaluation.api.dto;

import java.util.List;

/**
 * 评测用例保存请求。
 */
public record EvaluationCaseReq(
        String question,
        String expectedAnswer,
        List<String> expectedKeywords,
        List<Long> expectedChunkIds,
        String expectedStatus,
        String metadata,
        Boolean enabled
) {
}
