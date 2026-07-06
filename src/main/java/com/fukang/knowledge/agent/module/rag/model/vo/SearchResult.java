package com.fukang.knowledge.agent.module.rag.model.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RAG 检索候选结果。
 *
 * @param chunkId     文档块 ID
 * @param chunkText   文档块文本
 * @param similarity  当前排序分数，兼容旧字段
 * @param metadata    来源元数据
 * @param vectorScore 原始向量相似度分数
 * @param bm25Score   原始全文检索分数
 * @param rrfScore    RRF 融合分数
 * @param rerankScore 最终重排分数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {
    private Long chunkId;

    private String chunkText;

    private double similarity;

    private String metadata;

    private Double vectorScore;

    private Double bm25Score;

    private Double rrfScore;

    private Double rerankScore;

    /**
     * 返回带多路检索分数的新结果，并用最高优先级分数刷新 similarity。
     */
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
