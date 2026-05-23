package com.fukang.knowledge.agent.infrastructure.ai;

import com.fukang.knowledge.agent.common.enums.ModelTypeEnum;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.ModelConfigDO;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.ModelProviderDO;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 动态模型工厂
 * <p>根据数据库中的模型提供商和模型配置信息，编程式创建 langchain4j 原生模型实例。
 * 支持 OpenAI 兼容协议的 Chat 和 Embedding 模型，通过 Builder 模式配置
 * baseUrl、apiKey、modelName 等参数</p>
 */
@Slf4j
@Component
public class DynamicModelFactory {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);
    private static final int DEFAULT_MAX_RETRIES = 2;

    /**
     * 根据提供商和模型配置创建 ChatLanguageModel 实例
     *
     * @param provider 模型提供商信息
     * @param config   模型配置信息
     * @return ChatLanguageModel 实例
     */
    public ChatLanguageModel createChatModel(ModelProviderDO provider, ModelConfigDO config) {
        log.info("正在创建 Chat 模型实例: provider={}, model={}", provider.getName(), config.getModelName());
        return OpenAiChatModel.builder()
                .baseUrl(resolveBaseUrl(provider.getApiBaseUrl()))
                .apiKey(provider.getApiKey())
                .modelName(config.getModelName())
                .timeout(DEFAULT_TIMEOUT)
                .maxRetries(DEFAULT_MAX_RETRIES)
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    /**
     * 根据提供商和模型配置创建 EmbeddingModel 实例
     *
     * @param provider 模型提供商信息
     * @param config   模型配置信息
     * @return EmbeddingModel 实例
     */
    public EmbeddingModel createEmbeddingModel(ModelProviderDO provider, ModelConfigDO config) {
        log.info("正在创建 Embedding 模型实例: provider={}, model={}", provider.getName(), config.getModelName());
        return OpenAiEmbeddingModel.builder()
                .baseUrl(resolveBaseUrl(provider.getApiBaseUrl()))
                .apiKey(provider.getApiKey())
                .modelName(config.getModelName())
                .timeout(DEFAULT_TIMEOUT)
                .maxRetries(DEFAULT_MAX_RETRIES)
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    /**
     * 根据模型类型创建对应的模型实例
     */
    public Object createModel(ModelProviderDO provider, ModelConfigDO config, ModelTypeEnum modelType) {
        return switch (modelType) {
            case CHAT -> createChatModel(provider, config);
            case EMBEDDING -> createEmbeddingModel(provider, config);
            default -> throw new IllegalArgumentException("暂不支持的模型类型: " + modelType);
        };
    }

    /**
     * 解析 API 地址
     * <p>langchain4j/openai4j 将 API 路径（如 /embeddings）直接拼接到 baseUrl 后，
     * 因此默认地址需包含 /v1/ 路径前缀。用户自定义地址原样返回</p>
     */
    private String resolveBaseUrl(String baseUrl) {
        if (baseUrl != null && !baseUrl.isBlank()) {
            log.debug("使用自定义 API 地址: {}", baseUrl);
            return baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        }
        return "https://api.openai.com/v1/";
    }
}