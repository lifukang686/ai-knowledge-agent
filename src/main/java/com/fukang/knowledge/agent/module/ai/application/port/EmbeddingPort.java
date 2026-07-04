package com.fukang.knowledge.agent.module.ai.application.port;

import com.fukang.knowledge.agent.module.knowledge.domain.model.EmbeddingResult.EmbeddingVector;
import com.fukang.knowledge.agent.module.model.infrastructure.persistence.entity.ModelConfigDO;

import java.util.List;

/**
 * Embedding 模型调用端口。
 * <p>批处理策略由应用层控制，具体 SDK 调用由基础设施适配。</p>
 */
public interface EmbeddingPort {

    /**
     * 批量生成文本向量。
     */
    BatchResult embedBatch(ModelConfigDO modelConfig, List<String> texts, int chunkOrderOffset);

    /**
     * 批量向量化结果。
     */
    record BatchResult(List<EmbeddingVector> vectors, int totalTokens) {
    }
}
