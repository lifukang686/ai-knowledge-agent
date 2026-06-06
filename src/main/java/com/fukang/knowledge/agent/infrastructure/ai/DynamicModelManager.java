package com.fukang.knowledge.agent.infrastructure.ai;

import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.enums.ModelTypeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.infrastructure.config.DynamicModelProperties;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.ModelConfigDO;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.ModelProviderDO;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 动态模型管理器。
 * <p>使用 Caffeine 本地缓存管理动态创建的 langchain4j 原生模型实例。
 * 缓存以 "providerId:modelName" 为键，在 TTL 过期后自动从数据库刷新配置。</p>
 */
@Slf4j
@Component
public class DynamicModelManager {

    private final DynamicModelFactory modelFactory;
    private final DynamicModelProperties properties;
    private final ModelResolutionService resolutionService;

    private Cache<String, ChatLanguageModel> chatModelCache;
    private Cache<String, StreamingChatLanguageModel> streamingChatModelCache;
    private Cache<String, EmbeddingModel> embeddingModelCache;

    public DynamicModelManager(DynamicModelFactory modelFactory,
                               DynamicModelProperties properties,
                               ModelResolutionService resolutionService) {
        this.modelFactory = modelFactory;
        this.properties = properties;
        this.resolutionService = resolutionService;
    }

    @PostConstruct
    void initCaches() {
        Caffeine<Object, Object> cacheBuilder = Caffeine.newBuilder()
                .expireAfterWrite(properties.getCacheTtlSeconds(), TimeUnit.SECONDS)
                .maximumSize(properties.getCacheMaxSize());

        chatModelCache = cacheBuilder.build();
        streamingChatModelCache = cacheBuilder.build();
        embeddingModelCache = cacheBuilder.build();

        log.info("动态模型管理器初始化完成: cacheTtl={}s, maxSize={}",
                properties.getCacheTtlSeconds(), properties.getCacheMaxSize());
    }

    /**
     * 获取 ChatLanguageModel 实例。
     * <p>优先从缓存获取，缓存未命中时从数据库加载配置并创建新实例。</p>
     *
     * @return ChatLanguageModel 实例
     */
    public ChatLanguageModel getChatModel() {
        ModelProviderDO provider = resolutionService.resolveProvider();
        ModelConfigDO config = resolutionService.resolveModelConfig(provider.getId(), ModelTypeEnum.CHAT);
        String cacheKey = buildCacheKey(provider.getId(), config.getModelName());
        return chatModelCache.get(cacheKey, key -> {
            log.info("缓存未命中，创建新的 ChatLanguageModel 实例: key={}", key);
            try {
                return modelFactory.createChatModel(provider, config);
            } catch (Exception e) {
                log.error("创建 ChatLanguageModel 实例失败: provider={}, model={}",
                        provider.getName(), config.getModelName(), e);
                throw new BaseException(ErrorCodeEnum.MODEL_CREATION_FAILED);
            }
        });
    }

    /**
     * 获取流式 ChatLanguageModel 实例。
     * <p>优先从缓存获取，缓存未命中时从数据库加载配置并创建新实例。</p>
     *
     * @return StreamingChatLanguageModel 实例
     */
    public StreamingChatLanguageModel getStreamingChatModel() {
        ModelProviderDO provider = resolutionService.resolveProvider();
        ModelConfigDO config = resolutionService.resolveModelConfig(provider.getId(), ModelTypeEnum.CHAT);
        String cacheKey = buildCacheKey(provider.getId(), config.getModelName());
        return streamingChatModelCache.get(cacheKey, key -> {
            log.info("缓存未命中，创建新的 StreamingChatLanguageModel 实例: key={}", key);
            try {
                return modelFactory.createStreamingChatModel(provider, config);
            } catch (Exception e) {
                log.error("创建 StreamingChatLanguageModel 实例失败: provider={}, model={}",
                        provider.getName(), config.getModelName(), e);
                throw new BaseException(ErrorCodeEnum.MODEL_CREATION_FAILED);
            }
        });
    }

    /**
     * 获取 EmbeddingModel 实例。
     * <p>调用方已解析模型提供商和模型配置，当前方法只负责缓存复用和实例创建。</p>
     *
     * @param config   模型配置
     * @return EmbeddingModel 实例
     */
    public EmbeddingModel getEmbeddingModel(ModelConfigDO config) {
        ModelProviderDO provider = resolutionService.getModelProviderById(config.getProviderId());
        String cacheKey = buildCacheKey(provider.getId(), config.getModelName());
        return embeddingModelCache.get(cacheKey, key -> {
            log.info("缓存未命中，创建新的 EmbeddingModel 实例: key={}", key);
            try {
                return modelFactory.createEmbeddingModel(provider, config);
            } catch (Exception e) {
                log.error("创建 EmbeddingModel 实例失败: provider={}, model={}",
                        provider.getName(), config.getModelName(), e);
                throw new BaseException(ErrorCodeEnum.MODEL_CREATION_FAILED);
            }
        });
    }

    /**
     * 构建模型实例缓存键。
     */
    private String buildCacheKey(Long providerId, String modelName) {
        return providerId + ":" + modelName;
    }
}
