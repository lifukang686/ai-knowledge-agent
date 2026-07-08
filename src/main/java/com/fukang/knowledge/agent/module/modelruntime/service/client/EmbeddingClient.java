package com.fukang.knowledge.agent.module.modelruntime.service.client;

import com.fukang.knowledge.agent.module.model.model.entity.ModelConfigEntity;
import com.fukang.knowledge.agent.module.modelruntime.model.vo.EmbeddingBatchResult;

import java.util.List;

/**
 * Embedding 模型调用端口。
 * <p>批处理策略由应用层控制，具体 SDK 调用由基础设施适配。</p>
 */
public interface EmbeddingClient {

    /**
     * 批量生成文本向量。
     */
    EmbeddingBatchResult embedBatch(ModelConfigEntity modelConfig, List<String> texts, int chunkOrderOffset);
}
