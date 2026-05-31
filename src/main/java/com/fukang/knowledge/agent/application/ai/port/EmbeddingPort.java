package com.fukang.knowledge.agent.application.ai.port;

import com.fukang.knowledge.agent.domain.knowledge.model.EmbeddingResult.EmbeddingVector;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.ModelConfigDO;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.ModelProviderDO;

import java.util.List;

/**
 * Embedding 模型调用端口。
 * <p>批处理策略由应用层控制，具体 SDK 调用由基础设施适配。</p>
 */
public interface EmbeddingPort {

    BatchResult embedBatch(ModelProviderDO provider, ModelConfigDO modelConfig,
                           List<String> texts, int chunkOrderOffset);

    record BatchResult(List<EmbeddingVector> vectors, int totalTokens) {
    }
}
