package com.fukang.knowledge.agent.module.agent.model.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent 与 LLM 交互的通用消息模型。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentChatMessage {
    private Role role;

    private String content;

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
