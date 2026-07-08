package com.fukang.knowledge.agent.module.modelruntime.service.manager;

import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.enums.ModelTypeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.infrastructure.config.DynamicModelProperties;
import com.fukang.knowledge.agent.module.model.model.entity.ModelConfigEntity;
import com.fukang.knowledge.agent.module.model.model.entity.ModelProviderEntity;
import com.fukang.knowledge.agent.module.modelruntime.service.factory.DynamicModelFactory;
import com.fukang.knowledge.agent.module.modelruntime.service.resolution.ModelResolutionService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.scoring.ScoringModel;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicModelManager {

    private final DynamicModelFactory modelFactory;
    private final DynamicModelProperties properties;
    private final ModelResolutionService resolutionService;

    private Cache<String, ChatLanguageModel> chatModelCache;
    private Cache<String, StreamingChatLanguageModel> streamingChatModelCache;
    private Cache<String, EmbeddingModel> embeddingModelCache;
    private Cache<String, ScoringModel> rerankModelCache;

    /**
     * 负责按模型用途缓存 LangChain4j 模型实例，避免每次请求都重复构建底层 HTTP 客户端。
     */
    @PostConstruct
    void initCaches() {
        Caffeine<Object, Object> cacheBuilder = Caffeine.newBuilder()
                .expireAfterWrite(properties.getCacheTtlSeconds(), TimeUnit.SECONDS)
                .maximumSize(properties.getCacheMaxSize());

        chatModelCache = cacheBuilder.build();
        streamingChatModelCache = cacheBuilder.build();
        embeddingModelCache = cacheBuilder.build();
        rerankModelCache = cacheBuilder.build();

        log.info("Dynamic model manager initialized: cacheTtl={}s, maxSize={}",
                properties.getCacheTtlSeconds(), properties.getCacheMaxSize());
    }

    /**
     * 获取默认提供商下的 Chat 模型；如果默认提供商没有对应模型，则由解析服务兜底选择全局可用模型。
     */
    public ChatLanguageModel getChatModel() {
        ModelProviderEntity provider = resolutionService.resolveProvider();
        ModelConfigEntity config = resolutionService.resolveModelConfig(provider.getId(), ModelTypeEnum.CHAT);
        String cacheKey = buildCacheKey(provider.getId(), config.getModelName());
        return chatModelCache.get(cacheKey, key -> {
            log.info("Chat model cache miss: key={}", key);
            try {
                return modelFactory.createChatModel(provider, config);
            } catch (Exception e) {
                log.error("Failed to create ChatLanguageModel: provider={}, model={}",
                        provider.getName(), config.getModelName(), e);
                throw new BaseException(ErrorCodeEnum.MODEL_CREATION_FAILED);
            }
        });
    }

    /**
     * 获取流式 Chat 模型。缓存键与非流式模型分开，避免不同 SDK 实例类型混用。
     */
    public StreamingChatLanguageModel getStreamingChatModel() {
        ModelProviderEntity provider = resolutionService.resolveProvider();
        ModelConfigEntity config = resolutionService.resolveModelConfig(provider.getId(), ModelTypeEnum.CHAT);
        String cacheKey = buildCacheKey(provider.getId(), config.getModelName());
        return streamingChatModelCache.get(cacheKey, key -> {
            log.info("Streaming chat model cache miss: key={}", key);
            try {
                return modelFactory.createStreamingChatModel(provider, config);
            } catch (Exception e) {
                log.error("Failed to create StreamingChatLanguageModel: provider={}, model={}",
                        provider.getName(), config.getModelName(), e);
                throw new BaseException(ErrorCodeEnum.MODEL_CREATION_FAILED);
            }
        });
    }

    /**
     * 获取指定模型配置对应的 Embedding 模型。
     * Embedding 场景通常已在上游选定具体模型，因此这里只按配置所属提供商创建和缓存。
     */
    public EmbeddingModel getEmbeddingModel(ModelConfigEntity config) {
        ModelProviderEntity provider = resolutionService.getModelProviderById(config.getProviderId());
        String cacheKey = buildCacheKey(provider.getId(), config.getModelName());
        return embeddingModelCache.get(cacheKey, key -> {
            log.info("Embedding model cache miss: key={}", key);
            try {
                return modelFactory.createEmbeddingModel(provider, config);
            } catch (Exception e) {
                log.error("Failed to create EmbeddingModel: provider={}, model={}",
                        provider.getName(), config.getModelName(), e);
                throw new BaseException(ErrorCodeEnum.MODEL_CREATION_FAILED);
            }
        });
    }

    /**
     * 获取 Rerank ScoringModel。
     * 未配置 RERANK 模型时会抛出业务异常，由 RerankClient 捕获后降级到本地重排。
     */
    public ScoringModel getRerankModel() {
        ModelProviderEntity provider = resolutionService.resolveProvider();
        ModelConfigEntity config = resolutionService.resolveModelConfig(provider.getId(), ModelTypeEnum.RERANK);
        String cacheKey = buildCacheKey(provider.getId(), config.getModelName());
        return rerankModelCache.get(cacheKey, key -> {
            log.info("Rerank model cache miss: key={}", key);
            try {
                return modelFactory.createRerankModel(provider, config);
            } catch (Exception e) {
                log.error("Failed to create Rerank ScoringModel: provider={}, model={}",
                        provider.getName(), config.getModelName(), e);
                throw new BaseException(ErrorCodeEnum.MODEL_CREATION_FAILED);
            }
        });
    }

    /**
     * providerId + modelName 足以区分当前系统内的模型实例配置。
     */
    private String buildCacheKey(Long providerId, String modelName) {
        return providerId + ":" + modelName;
    }
}
