package com.fukang.knowledge.agent.api.evaluation.dto;

/**
 * 评测召回片段响应。
 */
public record EvaluationChunkResp(
        Long chunkId,
        String chunkText,
        double similarity,
        String metadata,
        Double vectorScore,
        Double bm25Score,
        Double rrfScore,
        Double rerankScore
) {
}
