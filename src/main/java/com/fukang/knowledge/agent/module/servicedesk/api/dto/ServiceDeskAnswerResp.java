package com.fukang.knowledge.agent.module.servicedesk.api.dto;

import com.fukang.knowledge.agent.module.agent.domain.model.AgentRunEvent;

import java.util.List;

/**
 * 服务台问答响应。
 */
public record ServiceDeskAnswerResp(
        String answer,
        String intent,
        String serviceType,
        String status,
        Long runId,
        Long ticketId,
        String ticketNo,
        Long conversationId,
        Boolean approvalRequired,
        ServiceTicketResp pendingTicket,
        List<AgentRunEvent> events,
        Boolean feedbackSubmitted
) {
}
