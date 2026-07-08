package com.fukang.knowledge.agent.module.modelruntime.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Chat 模型调用消息。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    private ChatMessageRole role;

    private String content;

    /**
     * 创建系统消息。
     */
    public static ChatMessage system(String content) {
        return new ChatMessage(ChatMessageRole.SYSTEM, content);
    }

    /**
     * 创建用户消息。
     */
    public static ChatMessage user(String content) {
        return new ChatMessage(ChatMessageRole.USER, content);
    }

    /**
     * 创建助手消息。
     */
    public static ChatMessage ai(String content) {
        return new ChatMessage(ChatMessageRole.AI, content);
    }
}
