package com.fukang.knowledge.agent.application.evaluation.result;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 单条评测运行结果。
 */
public record EvaluationCaseRunResult(
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
        List<EvaluationChunkResult> retrievedChunks,
        List<EvaluationChunkResult> rerankedChunks,
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
