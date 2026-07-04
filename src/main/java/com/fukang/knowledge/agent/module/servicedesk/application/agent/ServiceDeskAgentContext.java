package com.fukang.knowledge.agent.module.servicedesk.application.agent;

import com.fukang.knowledge.agent.module.servicedesk.application.ServiceDeskStreamHandler;
import com.fukang.knowledge.agent.module.servicedesk.domain.ServiceType;

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
