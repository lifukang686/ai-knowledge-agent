package com.fukang.knowledge.agent.application.knowledge;

import com.fukang.knowledge.agent.application.knowledge.model.EmbeddingResult;
import com.fukang.knowledge.agent.application.knowledge.model.EmbeddingResult.EmbeddingVector;
import com.fukang.knowledge.agent.application.model.ModelAppService;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.enums.ModelTypeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.infrastructure.ai.DynamicModelManager;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.ModelConfigDO;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.ModelProviderDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 嵌入向量服务（动态模型版本）
 * <p>负责将文本数据转换为向量表示，支持动态从数据库解析嵌入模型配置</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final ModelAppService modelAppService;
    private final DynamicModelManager modelManager;

    /**
     * 对文本列表执行向量嵌入计算
     *
     * @param texts 待嵌入的文本列表
     * @return 嵌入计算结果，包含向量列表和元数据
     */
    public EmbeddingResult embed(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            log.warn("待嵌入的文本列表为空，无法执行向量化");
            throw new BaseException(ErrorCodeEnum.CHUNK_DATA_EMPTY);
        }
        ModelConfigDO embeddingModel = findEmbeddingModel();
        ModelProviderDO provider = modelManager.resolveProvider();
        log.info("使用嵌入模型 [{}] 对 {} 个文本块执行向量化", embeddingModel.getModelName(), texts.size());
        try {
            EmbeddingModel embeddingClient = modelManager.getEmbeddingModel(provider, embeddingModel);
            EmbeddingRequest request = new EmbeddingRequest(texts, null);
            EmbeddingResponse response = embeddingClient.call(request);
            return convertToEmbeddingResult(response, embeddingModel);
        } catch (Exception e) {
            log.error("向量嵌入计算失败: model={}, textCount={}", embeddingModel.getModelName(), texts.size(), e);
            throw new BaseException(ErrorCodeEnum.EMBEDDING_FAILED);
        }
    }

    /**
     * 查找可用的嵌入模型配置
     * <p>策略：默认提供商下的嵌入模型 → 全局嵌入模型</p>
     */
    private ModelConfigDO findEmbeddingModel() {
        ModelProviderDO defaultProvider = modelAppService.findDefaultProvider();
        if (defaultProvider != null) {
            List<ModelConfigDO> defaultProviderModels = modelAppService
                    .findModelsByProviderAndType(defaultProvider.getId(), ModelTypeEnum.EMBEDDING);
            if (!defaultProviderModels.isEmpty()) {
                ModelConfigDO model = defaultProviderModels.get(0);
                log.info("在默认提供商 [{}] 下找到嵌入模型 [{}]", defaultProvider.getName(), model.getModelName());
                return model;
            }
            log.info("默认提供商 [{}] 下未配置嵌入模型，尝试使用其他提供商的嵌入模型", defaultProvider.getName());
        } else {
            log.info("系统中未设置默认模型提供商，尝试使用任意提供商的嵌入模型");
        }
        List<ModelConfigDO> allEmbeddingModels = modelAppService.findModelsByType(ModelTypeEnum.EMBEDDING);
        if (allEmbeddingModels.isEmpty()) {
            log.error("系统中未找到任何嵌入模型配置");
            throw new BaseException(ErrorCodeEnum.NO_EMBEDDING_MODEL_AVAILABLE);
        }
        ModelConfigDO fallbackModel = allEmbeddingModels.get(0);
        log.info("使用备用嵌入模型 [{}]", fallbackModel.getModelName());
        return fallbackModel;
    }

    /**
     * 将 Spring AI 嵌入响应转换为业务 EmbeddingResult
     */
    private EmbeddingResult convertToEmbeddingResult(EmbeddingResponse response, ModelConfigDO embeddingModel) {
        List<org.springframework.ai.embedding.Embedding> aiEmbeddings = response.getResults();
        List<EmbeddingVector> vectors = new ArrayList<>(aiEmbeddings.size());
        for (int i = 0; i < aiEmbeddings.size(); i++) {
            org.springframework.ai.embedding.Embedding aiEmbedding = aiEmbeddings.get(i);
            float[] embeddingArray = aiEmbedding.getOutput();
            vectors.add(new EmbeddingVector(i, embeddingArray, embeddingArray.length));
        }
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("modelName", embeddingModel.getModelName());
        metadata.put("providerId", embeddingModel.getProviderId());
        metadata.put("vectorDimension", vectors.isEmpty() ? 0 : vectors.get(0).dimension());
        int totalTokens = 0;
        if (response.getMetadata() != null && response.getMetadata().getUsage() != null) {
            totalTokens = (int) response.getMetadata().getUsage().getTotalTokens();
        }
        log.info("向量化完成: model={}, chunkCount={}, dimension={}, totalTokens={}",
                embeddingModel.getModelName(), vectors.size(),
                vectors.isEmpty() ? 0 : vectors.get(0).dimension(), totalTokens);
        return EmbeddingResult.allSuccess(vectors, embeddingModel.getModelName(), totalTokens, metadata);
    }
}