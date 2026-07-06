package com.fukang.knowledge.agent.module.evaluation.model.resp;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 评测召回片段响应。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationChunkResp {
    private Long chunkId;

    private String chunkText;

    private double similarity;

    private String metadata;

    private Double vectorScore;

    private Double bm25Score;

    private Double rrfScore;

    private Double rerankScore;

}
