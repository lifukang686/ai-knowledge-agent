package com.fukang.knowledge.agent.module.modelruntime.model.vo;

import com.fukang.knowledge.agent.module.knowledge.model.vo.EmbeddingVector;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Embedding 模型批量向量化结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingBatchResult {
    private List<EmbeddingVector> vectors;

    private int totalTokens;
}
