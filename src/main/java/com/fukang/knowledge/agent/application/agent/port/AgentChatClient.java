package com.fukang.knowledge.agent.application.agent.port;

import com.fukang.knowledge.agent.domain.agent.model.AgentChatMessage;
import com.fukang.knowledge.agent.domain.agent.model.AgentChatSession;

import java.util.List;

/**
 * Agent Chat 模型调用端口。
 * <p>应用层通过该端口发送通用消息，LangChain4j 等具体 SDK 由基础设施层适配。</p>
 */
public interface AgentChatClient {

    AgentChatSession newSession(int maxMessages);

    AgentChatSession newDefaultSession();

    String generate(AgentChatSession session, List<AgentChatMessage> newMessages);
}
