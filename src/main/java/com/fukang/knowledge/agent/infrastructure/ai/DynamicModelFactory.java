package com.fukang.knowledge.agent.infrastructure.ai;

import com.fukang.knowledge.agent.common.enums.ModelTypeEnum;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.ModelConfigDO;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.ModelProviderDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 动态模型工厂
 * <p>根据数据库中的模型提供商和模型配置信息，编程式创建 Spring AI 模型实例。
 * 当前支持 OpenAI 兼容协议的 Chat 和 Embedding 模型</p>
 */
@Slf4j
@Component
public class DynamicModelFactory {

    /**
     * 根据提供商和模型配置创建 ChatModel 实例
     *
     * @param provider 模型提供商信息
     * @param config   模型配置信息
     * @return ChatModel 实例
     */
    public ChatModel createChatModel(ModelProviderDO provider, ModelConfigDO config) {
        log.info("正在创建 Chat 模型实例: provider={}, model={}", provider.getName(), config.getModelName());
        OpenAiApi api = createOpenAiApi(provider);
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(config.getModelName())
                .build();
        return new OpenAiChatModel(api, options);
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
        OpenAiApi api = createOpenAiApi(provider);
        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .model(config.getModelName())
                .build();
        return new OpenAiEmbeddingModel(api, MetadataMode.EMBED, options);
    }

    /**
     * 根据提供商信息创建 OpenAiApi 实例
     * <p>使用 RestClient 而非 WebClient，确保 API 调用的兼容性</p>
     */
    private OpenAiApi createOpenAiApi(ModelProviderDO provider) {
        String baseUrl = provider.getApiBaseUrl();
        String apiKey = provider.getApiKey();
        if (baseUrl != null &&!baseUrl.isBlank()) {
            log.debug("使用自定义 API 地址: {}", baseUrl);
            return new OpenAiApi(baseUrl, apiKey);
        }
        return new OpenAiApi(apiKey);
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
}