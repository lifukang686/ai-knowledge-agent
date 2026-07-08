package com.fukang.knowledge.agent.module.rag.model.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RAG 检索候选结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {
    /** 文档块 ID */
    private Long chunkId;

    /** 文档块文本 */
    private String chunkText;

    /** 当前排序分数，兼容旧字段 */
    private double similarity;

    /** 来源元数据 */
    private String metadata;

    /** 原始向量相似度分数 */
    private Double vectorScore;

    /** 原始全文检索分数 */
    private Double bm25Score;

    /** RRF 融合分数 */
    private Double rrfScore;

    /** 最终重排分数 */
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
