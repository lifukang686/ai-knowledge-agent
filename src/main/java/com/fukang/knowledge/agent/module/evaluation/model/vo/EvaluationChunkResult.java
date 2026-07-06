package com.fukang.knowledge.agent.module.evaluation.model.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 评测展示用召回片段。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationChunkResult {
    private Long chunkId;

    private String chunkText;

    private double similarity;

    private String metadata;

    private Double vectorScore;

    private Double bm25Score;

    private Double rrfScore;

    private Double rerankScore;

}
