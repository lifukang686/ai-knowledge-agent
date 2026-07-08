package com.fukang.knowledge.agent.module.knowledge.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单个文档块的向量嵌入结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingVector {
    private int chunkOrder;

    private float[] vector;

    private int dimension;
}
