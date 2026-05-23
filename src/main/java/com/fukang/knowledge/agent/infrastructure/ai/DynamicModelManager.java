package com.fukang.knowledge.agent.infrastructure.ai;

import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.enums.ModelTypeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.infrastructure.config.DynamicModelProperties;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.ModelConfigDO;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.ModelProviderDO;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalListener;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 动态模型管理器
 * <p>使用 Caffeine 本地缓存管理动态创建的 langchain4j 原生模型实例。
 * 缓存以 "providerId:modelType" 为键，在 TTL 过期后自动从数据库刷新配置</p>
 */
@Slf4j
@Component
public class DynamicModelManager {

    private final DynamicModelFactory modelFactory;
    private final DynamicModelProperties properties;
    private final ModelResolutionService resolutionService;

    private Cache<String, ChatLanguageModel> chatModelCache;

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
        RemovalListener<String, Object> removalListener = (key, value, cause) ->
                log.info("模型实例缓存失效: key={}, cause={}", key, cause);

        Caffeine<Object, Object> cacheBuilder = Caffeine.newBuilder()
                .expireAfterWrite(properties.getCacheTtlSeconds(), TimeUnit.SECONDS)
                .maximumSize(properties.getCacheMaxSize());

        chatModelCache = cacheBuilder.build();
        embeddingModelCache = cacheBuilder.build();

        log.info("动态模型管理器初始化完成: cacheTtl={}s, maxSize={}",
                properties.getCacheTtlSeconds(), properties.getCacheMaxSize());
    }

    /**
     * 获取 ChatLanguageModel 实例
     * <p>优先从缓存获取，缓存未命中时从数据库加载配置并创建新实例</p>
     *
     * @param modelType 模型类型
     * @return ChatLanguageModel 实例
     */
    public ChatLanguageModel getChatModel(ModelTypeEnum modelType) {
        ModelProviderDO provider = resolutionService.resolveProvider();
        ModelConfigDO config = resolutionService.resolveModelConfig(provider.getId(), modelType);
        String cacheKey = buildCacheKey(provider.getId(), config.getModelName());
        return chatModelCache.get(cacheKey, key -> {
            log.info("缓存未命中，创建新 ChatLanguageModel 实例: key={}", key);
            try {
                return modelFactory.createChatModel(provider, config);
            } catch (Exception e) {
                log.error("创建 ChatLanguageModel 实例失败: provider={}, model={}", provider.getName(), config.getModelName(), e);
                throw new BaseException(ErrorCodeEnum.MODEL_CREATION_FAILED);
            }
        });
    }

    /**
     * 获取 ChatLanguageModel 实例（指定模型ID）
     *
     * @param modelId 模型配置ID
     * @return ChatLanguageModel 实例
     */
    public ChatLanguageModel getChatModelById(Long modelId) {
        ModelConfigDO config = resolutionService.getModelConfigById(modelId);
        ModelProviderDO provider = resolutionService.getModelProviderById(config.getProviderId());
        String cacheKey = buildCacheKey(provider.getId(), config.getModelName());
        return chatModelCache.get(cacheKey, key -> {
            log.info("缓存未命中，按模型ID创建 ChatLanguageModel 实例: key={}", key);
            try {
                return modelFactory.createChatModel(provider, config);
            } catch (Exception e) {
                log.error("创建 ChatLanguageModel 实例失败: provider={}, model={}", provider.getName(), config.getModelName(), e);
                throw new BaseException(ErrorCodeEnum.MODEL_CREATION_FAILED);
            }
        });
    }

    /**
     * 获取 EmbeddingModel 实例
     * <p>优先从缓存获取，缓存未命中时从数据库加载配置并创建新实例</p>
     *
     * @param modelType 模型类型
     * @return EmbeddingModel 实例
     */
    public EmbeddingModel getEmbeddingModel(ModelTypeEnum modelType) {
        ModelProviderDO provider = resolutionService.resolveProvider();
        ModelConfigDO config = resolutionService.resolveModelConfig(provider.getId(), modelType);
        String cacheKey = buildCacheKey(provider.getId(), config.getModelName());
        return embeddingModelCache.get(cacheKey, key -> {
            log.info("缓存未命中，创建新 EmbeddingModel 实例: key={}", key);
            try {
                return modelFactory.createEmbeddingModel(provider, config);
            } catch (Exception e) {
                log.error("创建 EmbeddingModel 实例失败: provider={}, model={}", provider.getName(), config.getModelName(), e);
                throw new BaseException(ErrorCodeEnum.MODEL_CREATION_FAILED);
            }
        });
    }

    /**
     * 获取 EmbeddingModel 实例（指定提供商和模型配置）
     *
     * @param provider 模型提供商
     * @param config   模型配置
     * @return EmbeddingModel 实例
     */
    public EmbeddingModel getEmbeddingModel(ModelProviderDO provider, ModelConfigDO config) {
        String cacheKey = buildCacheKey(provider.getId(), config.getModelName());
        return embeddingModelCache.get(cacheKey, key -> {
            log.info("缓存未命中，创建新 EmbeddingModel 实例: key={}", key);
            try {
                return modelFactory.createEmbeddingModel(provider, config);
            } catch (Exception e) {
                log.error("创建 EmbeddingModel 实例失败: provider={}, model={}", provider.getName(), config.getModelName(), e);
                throw new BaseException(ErrorCodeEnum.MODEL_CREATION_FAILED);
            }
        });
    }

    /**
     * 获取用于 Chat 调用的模型名称（用于日志记录）
     *
     * @param modelType 模型类型
     * @return 模型名称
     */
    public String resolveModelName(ModelTypeEnum modelType) {
        ModelProviderDO provider = resolutionService.resolveProvider();
        ModelConfigDO config = resolutionService.resolveModelConfig(provider.getId(), modelType);
        return config.getModelName();
    }

    /**
     * 获取模型提供商信息（供外部使用）
     */
    public ModelProviderDO resolveProvider() {
        return resolutionService.resolveProvider();
    }

    /**
     * 获取模型配置信息（供外部使用）
     */
    public ModelConfigDO resolveModelConfig(Long providerId, ModelTypeEnum modelType) {
        return resolutionService.resolveModelConfig(providerId, modelType);
    }

    /**
     * 手动清除所有缓存
     */
    public void evictAllCaches() {
        chatModelCache.invalidateAll();
        embeddingModelCache.invalidateAll();
        log.info("已清除所有动态模型缓存");
    }

    /**
     * 手动清除指定模型的缓存
     */
    public void evictCache(Long providerId, String modelName) {
        String cacheKey = buildCacheKey(providerId, modelName);
        chatModelCache.invalidate(cacheKey);
        embeddingModelCache.invalidate(cacheKey);
        log.info("已清除模型缓存: key={}", cacheKey);
    }

    /**
     * 构建缓存键
     */
    private String buildCacheKey(Long providerId, String modelName) {
        return providerId + ":" + modelName;
    }
}