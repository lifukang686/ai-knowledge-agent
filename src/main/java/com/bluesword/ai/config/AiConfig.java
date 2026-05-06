package com.bluesword.ai.config;

import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Bean
    public OpenAiEmbeddingModel openAiEmbeddingModel(@Value("${spring.ai.openai.api-key}") String apiKey) {
        // 1. 使用 Builder 创建 OpenAiApi 实例
        OpenAiApi openAiApi = OpenAiApi.builder()
                .apiKey(apiKey) // 设置 API Key
                // ⭐ 这里是关键！若需要对接其他 API（如 DeepSeek），设置 baseUrl⭐
                // .baseUrl("https://api.deepseek.com/v1")
                .build();

        // 2. 通过构造函数实例化 OpenAiEmbeddingModel
        return new OpenAiEmbeddingModel(openAiApi);
    }
}