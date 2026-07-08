package com.fukang.knowledge.agent.module.modelruntime.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Rerank 模型对单个候选片段的评分结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RerankScore {
    private int index;

    private Long chunkId;

    private double score;
}
