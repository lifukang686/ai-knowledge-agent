package com.fukang.knowledge.agent.module.modelruntime.service.factory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fukang.knowledge.agent.module.model.model.entity.ModelConfigEntity;
import com.fukang.knowledge.agent.module.model.model.entity.ModelProviderEntity;
import com.fukang.knowledge.agent.module.modelruntime.service.client.impl.HttpScoringModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.scoring.ScoringModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicModelFactory {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);
    private static final int DEFAULT_MAX_RETRIES = 2;
    private static final String DEFAULT_OPENAI_BASE_URL = "https://api.openai.com/v1/";

    private final ObjectMapper objectMapper;

    /**
     * 根据数据库中的提供商和模型配置创建非流式对话模型。
     * 当前统一走 OpenAI-compatible 协议，具体 baseUrl/apiKey/modelName 由用户配置决定。
     */
    public ChatLanguageModel createChatModel(ModelProviderEntity provider, ModelConfigEntity config) {
        log.info("Creating Chat model: provider={}, model={}", provider.getName(), config.getModelName());
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
     * 创建流式对话模型，供 SSE 问答等需要增量 token 输出的场景使用。
     */
    public StreamingChatLanguageModel createStreamingChatModel(ModelProviderEntity provider, ModelConfigEntity config) {
        log.info("Creating Streaming Chat model: provider={}, model={}", provider.getName(), config.getModelName());
        return OpenAiStreamingChatModel.builder()
                .baseUrl(resolveBaseUrl(provider.getApiBaseUrl()))
                .apiKey(provider.getApiKey())
                .modelName(config.getModelName())
                .timeout(DEFAULT_TIMEOUT)
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    /**
     * 创建 Embedding 模型。
     * defaultParams 中可配置 dimensions，用于 text-embedding-3 等支持指定维度的模型。
     */
    public EmbeddingModel createEmbeddingModel(ModelProviderEntity provider, ModelConfigEntity config) {
        Integer dimensions = parseDimensions(config);
        log.info("Creating Embedding model: provider={}, model={}, dimensions={}",
                provider.getName(), config.getModelName(), dimensions);

        OpenAiEmbeddingModel.OpenAiEmbeddingModelBuilder builder = OpenAiEmbeddingModel.builder()
                .baseUrl(resolveBaseUrl(provider.getApiBaseUrl()))
                .apiKey(provider.getApiKey())
                .modelName(config.getModelName())
                .timeout(DEFAULT_TIMEOUT)
                .maxRetries(DEFAULT_MAX_RETRIES)
                .logRequests(true)
                .logResponses(true);

        if (dimensions != null) {
            builder.dimensions(dimensions);
        }
        return builder.build();
    }

    /**
     * 创建重排序模型适配器。
     * Rerank 服务没有统一的 OpenAI SDK 抽象，这里用 HttpScoringModel 统一封装 HTTP 调用。
     */
    public ScoringModel createRerankModel(ModelProviderEntity provider, ModelConfigEntity config) {
        log.info("Creating Rerank model: provider={}, model={}", provider.getName(), config.getModelName());
        return new HttpScoringModel(provider, config, objectMapper);
    }

    /**
     * 从模型默认参数中读取 embedding 维度；解析失败时不阻断模型创建，沿用模型默认维度。
     */
    private Integer parseDimensions(ModelConfigEntity config) {
        String params = config.getDefaultParams();
        if (params == null || params.isBlank()) {
            return null;
        }

        try {
            Map<String, Object> map = objectMapper.readValue(params, new TypeReference<Map<String, Object>>() {});
            Object dimensions = map.get("dimensions");
            return dimensions instanceof Number number ? number.intValue() : null;
        } catch (Exception e) {
            log.debug("Failed to parse embedding dimensions from model defaultParams: model={}",
                    config.getModelName(), e);
            return null;
        }
    }

    /**
     * LangChain4j 会在 baseUrl 后拼接具体 API 路径，因此这里保证地址以 / 结尾。
     */
    private String resolveBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return DEFAULT_OPENAI_BASE_URL;
        }
        return baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }
}
