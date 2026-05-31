package com.fukang.knowledge.agent.application.knowledge.embedding;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fukang.knowledge.agent.application.ai.port.EmbeddingPort;
import com.fukang.knowledge.agent.domain.knowledge.model.EmbeddingResult;
import com.fukang.knowledge.agent.domain.knowledge.model.EmbeddingResult.EmbeddingVector;
import com.fukang.knowledge.agent.application.model.ModelAppService;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.enums.ModelTypeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.ModelConfigDO;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.ModelProviderDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 嵌入向量服务（动态模型版本）
 * <p>负责将文本数据转换为向量表示，支持按模型配置的 maxBatchSize 分批调用 API，
 * 解决部分模型（如 text-embedding-v3 最大 10 条/次）的批量限制问题</p>
 */
@Slf4j
@Service
public class EmbeddingService {

    /** 未配置 maxBatchSize 时的默认每批最大文本数 */
    private static final int DEFAULT_MAX_BATCH_SIZE = 100;

    private final ModelAppService modelAppService;
    private final EmbeddingPort embeddingPort;
    private final ObjectMapper objectMapper;

    public EmbeddingService(ModelAppService modelAppService,
                            EmbeddingPort embeddingPort,
                            ObjectMapper objectMapper) {
        this.modelAppService = modelAppService;
        this.embeddingPort = embeddingPort;
        this.objectMapper = objectMapper;
    }

    /**
     * 对文本列表执行向量嵌入计算，根据模型配置的 maxBatchSize 自动分批调用
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
        ModelProviderDO provider = resolveProvider();
        int maxBatchSize = parseMaxBatchSize(embeddingModel);

        log.info("使用嵌入模型 [{}] 对 {} 个文本块分批向量化，batchSize={}",
                embeddingModel.getModelName(), texts.size(), maxBatchSize);

        try {
            List<List<String>> batches = partition(texts, maxBatchSize);
            String apiUrl = provider.getApiBaseUrl();
            if (apiUrl == null || apiUrl.isBlank()) {
                throw new BaseException(ErrorCodeEnum.MODEL_BASE_URL_IS_NULL);
            }

            List<EmbeddingVector> allVectors = new ArrayList<>(texts.size());
            int totalTokens = 0;

            for (int batchIndex = 0; batchIndex < batches.size(); batchIndex++) {
                List<String> batch = batches.get(batchIndex);
                int batchOffset = batchIndex * maxBatchSize;

                log.info("嵌入 API 批次 {}/{}: baseUrl={}, model={}, textCount={}, offset={}",
                        batchIndex + 1, batches.size(), apiUrl,
                        embeddingModel.getModelName(), batch.size(), batchOffset);

                // batchOffset 用于把批次内序号还原为文档 chunkOrder。
                EmbeddingPort.BatchResult batchResult = embeddingPort.embedBatch(
                        provider, embeddingModel, batch, batchOffset);
                allVectors.addAll(batchResult.vectors());
                totalTokens += batchResult.totalTokens();
            }

            return buildBatchResult(allVectors, embeddingModel, totalTokens);
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            String apiUrl = provider.getApiBaseUrl();
            if (apiUrl == null || apiUrl.isBlank()) {
                apiUrl = "https://api.openai.com";
            }
            String detailMsg = String.format(
                    "向量嵌入计算失败: baseUrl=%s, model=%s, textCount=%d, error=%s",
                    apiUrl, embeddingModel.getModelName(), texts.size(), e.getMessage());
            log.error(detailMsg, e);
            throw new BaseException(ErrorCodeEnum.EMBEDDING_FAILED);
        }
    }

    /**
     * 从模型配置的 defaultParams 中解析 maxBatchSize
     * <p>未配置或解析失败时使用默认值 100</p>
     */
    private int parseMaxBatchSize(ModelConfigDO config) {
        String params = config.getDefaultParams();
        if (params == null || params.isBlank()) {
            return DEFAULT_MAX_BATCH_SIZE;
        }
        try {
            Map<String, Object> map = objectMapper.readValue(params,
                    new TypeReference<Map<String, Object>>() {});
            Object maxBatchSize = map.get("maxBatchSize");
            if (maxBatchSize instanceof Number num) {
                return num.intValue();
            }
        } catch (Exception e) {
            log.debug("解析模型 [{}] 的 defaultParams 中 maxBatchSize 失败，使用默认值 {}",
                    config.getModelName(), DEFAULT_MAX_BATCH_SIZE);
        }
        return DEFAULT_MAX_BATCH_SIZE;
    }

    /**
     * 将文本列表按 maxBatchSize 分批
     */
    private List<List<String>> partition(List<String> texts, int maxBatchSize) {
        if (texts.size() <= maxBatchSize) {
            return List.of(texts);
        }
        List<List<String>> batches = new ArrayList<>();
        for (int i = 0; i < texts.size(); i += maxBatchSize) {
            int end = Math.min(i + maxBatchSize, texts.size());
            batches.add(texts.subList(i, end));
        }
        return batches;
    }

    /**
     * 根据所有批次的合并结果构建 EmbeddingResult
     */
    private EmbeddingResult buildBatchResult(List<EmbeddingVector> allVectors,
                                             ModelConfigDO embeddingModel,
                                             int totalTokens) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("modelName", embeddingModel.getModelName());
        metadata.put("modelId", embeddingModel.getId());
        metadata.put("providerId", embeddingModel.getProviderId());
        metadata.put("vectorDimension", allVectors.isEmpty() ? 0 : allVectors.get(0).dimension());
        // 当前没有独立版本字段，先用 modelName 作为可审计版本标识。
        metadata.put("modelVersion", embeddingModel.getModelName());

        log.info("向量化完成: model={}, chunkCount={}, dimension={}, totalTokens={}",
                embeddingModel.getModelName(), allVectors.size(),
                allVectors.isEmpty() ? 0 : allVectors.get(0).dimension(), totalTokens);
        return EmbeddingResult.allSuccess(allVectors, embeddingModel.getModelName(), totalTokens, metadata);
    }

    private ModelProviderDO resolveProvider() {
        ModelProviderDO defaultProvider = modelAppService.findDefaultProvider();
        if (defaultProvider != null) {
            return defaultProvider;
        }
        List<ModelProviderDO> allProviders = modelAppService.listProviders();
        if (allProviders.isEmpty()) {
            log.error("系统中未配置任何模型提供商");
            throw new BaseException(ErrorCodeEnum.NO_MODEL_PROVIDER_AVAILABLE);
        }
        log.info("系统中未设置默认模型提供商，使用第一个可用提供商 [{}]", allProviders.get(0).getName());
        return allProviders.get(0);
    }

    /**
     * 查找可用的嵌入模型配置。
     * <p>策略：默认提供商下的嵌入模型 → 全局嵌入模型。</p>
     */
    private ModelConfigDO findEmbeddingModel() {
        // 使用动态模型配置：默认 provider 下的 embedding 模型优先。
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
}
