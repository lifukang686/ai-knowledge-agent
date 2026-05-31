package com.fukang.knowledge.agent.application.ai.port;

import java.util.List;

/**
 * Chat 模型调用端口。
 * <p>应用层只表达消息与返回文本，不直接依赖具体 LLM SDK。</p>
 */
public interface ChatCompletionPort {

    String complete(List<Message> messages);

    record Message(Role role, String content) {
        public static Message system(String content) {
            return new Message(Role.SYSTEM, content);
        }

        public static Message user(String content) {
            return new Message(Role.USER, content);
        }

        public static Message ai(String content) {
            return new Message(Role.AI, content);
        }
    }

    enum Role {
        SYSTEM,
        USER,
        AI
    }
}
