package com.fukang.knowledge.agent.module.servicedesk.api.dto;

/**
 * 服务台问答请求。
 */
public record ServiceDeskAskReq(
        String question,
        String serviceType,
        Long knowledgeBaseId,
        Long conversationId
) {
}
