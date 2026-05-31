package com.fukang.knowledge.agent.domain.agent.model;

/**
 * Agent 与 LLM 交互的通用消息模型。
 */
public record AgentChatMessage(Role role, String content) {

    public enum Role {
        SYSTEM,
        USER,
        AI
    }

    public static AgentChatMessage system(String content) {
        return new AgentChatMessage(Role.SYSTEM, content);
    }

    public static AgentChatMessage user(String content) {
        return new AgentChatMessage(Role.USER, content);
    }

    public static AgentChatMessage ai(String content) {
        return new AgentChatMessage(Role.AI, content);
    }
}
