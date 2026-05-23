package com.fukang.knowledge.agent.infrastructure.ai;

import com.fukang.knowledge.agent.common.enums.ModelTypeEnum;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * LangChain4j 客户端实现（动态模型版本）
 * <p>基于 LangChain4j 封装 AI 模型调用能力，通过 DynamicModelManager 动态获取模型实例。
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
     * <p>动态获取 CHAT 类型模型实例，使用 LangChain4j 统一接口调用</p>
     *
     * @param promptText 提示词文本
     * @return Response<AiMessage> 包含 AI 响应内容和 Token 使用量等元数据
     */
    public Response<AiMessage> call(String promptText) {
        ChatLanguageModel chatModel = modelManager.getChatModel(ModelTypeEnum.CHAT);
        return chatModel.generate(UserMessage.from(promptText));
    }

    /**
     * 调用嵌入模型，将文本列表转换为向量列表
     * <p>动态获取 EMBEDDING 类型模型实例进行向量化计算</p>
     *
     * @param texts 待嵌入的文本列表
     * @return Response<List<Embedding>> 包含向量列表和元数据
     */
    public Response<List<Embedding>> embed(List<String> texts) {
        dev.langchain4j.model.embedding.EmbeddingModel embeddingModel = modelManager.getEmbeddingModel(ModelTypeEnum.EMBEDDING);
        List<TextSegment> segments = texts.stream().map(TextSegment::from).toList();
        return embeddingModel.embedAll(segments);
    }
}