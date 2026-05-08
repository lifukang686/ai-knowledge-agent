package com.fukang.knowledge.agent.infrastructure.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

/**
 * Spring AI 客户端实现
 * <p>基于 Spring AI 的 {@link ChatClient} 封装 AI 模型调用能力，
 * 为上层业务提供统一的模型调用入口，屏蔽底层模型差异</p>
 */
@Service
public class SpringAiClientImpl {

    /** Spring AI 聊天客户端，由 Spring AI 自动配置注入 */
    private final ChatClient chatClient;

    /**
     * 构造方法，通过 ChatClient.Builder 构建客户端实例
     * <p>当前使用默认注入的 ChatClient，企业级项目中可根据数据库配置动态创建不同的 ChatModel 实例</p>
     *
     * @param builder ChatClient 构建器，由 Spring AI 自动注入
     */
    public SpringAiClientImpl(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    /**
     * 调用 AI 模型并返回完整响应
     * <p>当前 MVP 阶段直接使用默认模型，后续可通过 chatClient.prompt().options(...)
     * 传入指定的 modelName 等动态参数实现多模型切换</p>
     *
     * @param promptText 用户输入的提示词
     * @param modelName  模型名称（当前未使用，预留多模型切换）
     * @return AI 模型的完整响应，包含文本内容和 Token 消耗等元数据
     */
    public ChatResponse call(String promptText, String modelName) {
        return chatClient.prompt(new Prompt(promptText))
                .call()
                .chatResponse();
    }
}
