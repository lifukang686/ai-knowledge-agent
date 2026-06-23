package com.fukang.knowledge.agent.api.evaluation.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 评测单条运行结果响应。
 */
public record EvaluationCaseResultResp(
        Long id,
        Long runId,
        Long caseId,
        String question,
        String expectedAnswer,
        String actualAnswer,
        String rewrittenQuery,
        String expectedStatus,
        String actualStatus,
        List<String> expectedKeywords,
        List<Long> expectedChunkIds,
        List<EvaluationChunkResp> retrievedChunks,
        List<EvaluationChunkResp> rerankedChunks,
        Double retrievalHitScore,
        Double keywordScore,
        Double statusScore,
        Double totalScore,
        Boolean passed,
        Map<String, Object> metricDetail,
        Long latencyMs,
        String errorMessage,
        LocalDateTime createTime
) {
}
