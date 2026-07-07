package com.fukang.knowledge.agent.module.modelruntime.service.client;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fukang.knowledge.agent.module.knowledge.model.vo.EmbeddingResult.EmbeddingVector;
import com.fukang.knowledge.agent.module.model.model.entity.ModelConfigEntity;

import java.util.List;

/**
 * Embedding 模型调用端口。
 * <p>批处理策略由应用层控制，具体 SDK 调用由基础设施适配。</p>
 */
public interface EmbeddingClient {

    /**
     * 批量生成文本向量。
     */
    BatchResult embedBatch(ModelConfigEntity modelConfig, List<String> texts, int chunkOrderOffset);

    /**
     * 批量向量化结果。
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class BatchResult {
        private List<EmbeddingVector> vectors;

        private int totalTokens;

    }
}
