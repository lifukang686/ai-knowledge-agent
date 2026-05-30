package com.fukang.knowledge.agent.infrastructure.ai;

import com.fukang.knowledge.agent.infrastructure.config.AgentProperties;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.springframework.stereotype.Component;

/**
 * ChatMemory 工厂
 * <p>按需创建对话记忆实例，每个 Agent 运行实例独立一个 ChatMemory，防止跨任务混淆。
 *
 * <pre>
 * ChatMemory  vs  AgentContext 分工：
 * ┌─────────────────────┬──────────────────────────┐
 * │ ChatMemory          │ AgentContext             │
 * ├─────────────────────┼──────────────────────────┤
 * │ LLM 对话历史         │ Agent 执行状态            │
 * │ System/User/AI 消息  │ 计划、步骤、状态机         │
 * │ → 驱动 LLM 上下文窗口 │ → 驱动执行流程             │
 * │ → 内存/短期          │ → 持久化到 agent_run       │
 * │ → 我"记得"什么       │ → 我"执行到"哪一步          │
 * └─────────────────────┴──────────────────────────┘
 * </pre>
 * </p>
 */
@Component
public class AgentMemoryFactory {

    private final AgentProperties agentProperties;

    public AgentMemoryFactory(AgentProperties agentProperties) {
        this.agentProperties = agentProperties;
    }

    /**
     * 创建基于消息数量的滑动窗口 ChatMemory
     *
     * @param maxMessages 最大保留消息数（超出后旧消息自动丢弃）
     * @return ChatMemory 实例
     */
    public ChatMemory createMessageWindowMemory(int maxMessages) {
        return MessageWindowChatMemory.withMaxMessages(maxMessages);
    }

    /**
     * 使用默认配置创建 ChatMemory
     * <p>默认最大消息数从 {@code knowledge-agent.agent.chat-memory-max-messages} 读取</p>
     */
    public ChatMemory createDefault() {
        return createMessageWindowMemory(agentProperties.getChatMemoryMaxMessages());
    }
}