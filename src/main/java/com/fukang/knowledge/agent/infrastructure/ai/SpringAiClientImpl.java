package com.fukang.knowledge.agent.infrastructure.ai;

import com.fukang.knowledge.agent.common.enums.ModelTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Spring AI 客户端实现（动态模型版本）
 * <p>基于 Spring AI 封装 AI 模型调用能力，通过 DynamicModelManager 动态获取模型实例。
 * 支持 Chat 和 Embedding 两种调用模式，为上层业务提供统一的模型调用入口</p>
 */
@Slf4j
@Service
public class SpringAiClientImpl {

    private final DynamicModelManager modelManager;

    public SpringAiClientImpl(DynamicModelManager modelManager) {
        this.modelManager = modelManager;
    }

    /**
     * 调用 AI 聊天模型并返回完整响应
     * <p>动态获取 CHAT 类型模型实例，使用 ChatClient 统一接口调用</p>
     *
     * @param promptText 提示词文本
     * @return ChatResponse 包含 AI 响应内容和 Token 使用量等元数据
     */
    public ChatResponse call(String promptText) {
        ChatModel chatModel = modelManager.getChatModel(ModelTypeEnum.CHAT);
        ChatClient chatClient = ChatClient.builder(chatModel).build();
        return chatClient.prompt(new Prompt(promptText))
                .call()
                .chatResponse();
    }

    /**
     * 调用嵌入模型，将文本列表转换为向量列表
     * <p>动态获取 EMBEDDING 类型模型实例进行向量化计算</p>
     *
     * @param texts 待嵌入的文本列表
     * @return EmbeddingResponse 包含向量列表和元数据
     */
    public EmbeddingResponse embed(List<String> texts) {
        EmbeddingModel embeddingModel = modelManager.getEmbeddingModel(ModelTypeEnum.EMBEDDING);
        EmbeddingRequest request = new EmbeddingRequest(texts, null);
        return embeddingModel.call(request);
    }
}