package com.fukang.knowledge.agent.module.agent.integration;

import com.fukang.knowledge.agent.module.agent.model.vo.AgentChatMessage;
import com.fukang.knowledge.agent.module.agent.model.bo.AgentChatSession;

import java.util.List;

/**
 * Agent Chat 模型调用端口。
 * <p>应用层通过该端口发送通用消息，LangChain4j 等具体 SDK 由基础设施层适配。</p>
 */
public interface AgentChatClient {

    /**
     * 创建指定窗口大小的 Agent 对话会话。
     */
    AgentChatSession newSession(int maxMessages);

    /**
     * 创建默认窗口大小的 Agent 对话会话。
     */
    AgentChatSession newDefaultSession();

    /**
     * 发送新消息并返回模型输出文本。
     */
    String generate(AgentChatSession session, List<AgentChatMessage> newMessages);
}
