package com.fukang.knowledge.agent.application.ai.port;

import java.util.List;

/**
 * Chat 模型调用端口。
 * <p>应用层只表达消息与返回文本，不直接依赖具体 LLM SDK。</p>
 */
public interface ChatCompletionPort {

    /**
     * 调用 Chat 模型生成完整文本。
     */
    String complete(List<Message> messages);

    /**
     * Chat 消息。
     */
    record Message(Role role, String content) {
        /**
         * 创建系统消息。
         */
        public static Message system(String content) {
            return new Message(Role.SYSTEM, content);
        }

        /**
         * 创建用户消息。
         */
        public static Message user(String content) {
            return new Message(Role.USER, content);
        }

        /**
         * 创建助手消息。
         */
        public static Message ai(String content) {
            return new Message(Role.AI, content);
        }
    }

    /**
     * Chat 消息角色。
     */
    enum Role {
        /**
         * 系统消息。
         */
        SYSTEM,
        /**
         * 用户消息。
         */
        USER,
        /**
         * 助手消息。
         */
        AI
    }
}
