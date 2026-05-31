package com.fukang.knowledge.agent.domain.agent.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent 推理过程中的滑动对话窗口。
 * <p>领域层只记录通用消息，不依赖具体 LLM SDK 的 Memory 实现。</p>
 */
public class AgentChatSession {

    private final int maxMessages;
    private final List<AgentChatMessage> messages = new ArrayList<>();

    public AgentChatSession(int maxMessages) {
        this.maxMessages = Math.max(maxMessages, 1);
    }

    public void add(AgentChatMessage message) {
        if (message == null) {
            return;
        }
        messages.add(message);
        trimToWindow();
    }

    public List<AgentChatMessage> messages() {
        return List.copyOf(messages);
    }

    private void trimToWindow() {
        while (messages.size() > maxMessages) {
            messages.remove(0);
        }
    }
}
