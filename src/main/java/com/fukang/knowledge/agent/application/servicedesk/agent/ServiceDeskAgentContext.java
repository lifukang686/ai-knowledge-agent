package com.fukang.knowledge.agent.application.servicedesk.agent;

import com.fukang.knowledge.agent.domain.servicedesk.ServiceType;

/**
 * 服务台 Agent 工具调用上下文。
 * <p>由服务台入口写入 ThreadLocal，工具自动读取，LLM 不能通过参数伪造用户身份。</p>
 */
public record ServiceDeskAgentContext(
        Long userId,
        Long runId,
        Long knowledgeBaseId,
        Long conversationId,
        ServiceType serviceType,
        String question
) {
}
