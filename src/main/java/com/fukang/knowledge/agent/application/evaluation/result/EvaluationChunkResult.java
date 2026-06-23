package com.fukang.knowledge.agent.application.evaluation.result;

/**
 * 评测展示用召回片段。
 */
public record EvaluationChunkResult(
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
