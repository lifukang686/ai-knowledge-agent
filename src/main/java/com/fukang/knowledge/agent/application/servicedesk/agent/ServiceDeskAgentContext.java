package com.fukang.knowledge.agent.application.servicedesk.agent;

import com.fukang.knowledge.agent.application.servicedesk.ServiceDeskStreamHandler;
import com.fukang.knowledge.agent.domain.servicedesk.ServiceType;

/**
 * 服务台 Agent 工具调用上下文。
 */
public record ServiceDeskAgentContext(
        Long userId,
        Long runId,
        Long knowledgeBaseId,
        Long conversationId,
        ServiceType serviceType,
        String question,
        ServiceDeskStreamHandler streamHandler
) {
}
