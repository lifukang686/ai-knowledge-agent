package com.fukang.knowledge.agent.domain.rag.model;

/**
 * RAG retrieval candidate with explicit score semantics.
 *
 * @param chunkId     document chunk id
 * @param chunkText   document chunk text
 * @param similarity  current ranking score kept for backward compatibility
 * @param metadata    source metadata
 * @param vectorScore raw vector similarity score
 * @param bm25Score   raw full-text/BM25-like score
 * @param rrfScore    reciprocal-rank-fusion score
 * @param rerankScore final rerank score
 */
public record SearchResult(
        Long chunkId,
        String chunkText,
        double similarity,
        String metadata,
        Double vectorScore,
        Double bm25Score,
        Double rrfScore,
        Double rerankScore
) {
    public SearchResult(Long chunkId, String chunkText, double similarity, String metadata) {
        this(chunkId, chunkText, similarity, metadata, null, null, null, null);
    }

    public SearchResult withScores(Double vectorScore, Double bm25Score, Double rrfScore, Double rerankScore) {
        double currentScore = rerankScore != null ? rerankScore
                : rrfScore != null ? rrfScore
                : vectorScore != null ? vectorScore
                : bm25Score != null ? bm25Score
                : similarity;
        return new SearchResult(chunkId, chunkText, currentScore, metadata,
                vectorScore, bm25Score, rrfScore, rerankScore);
    }
}
