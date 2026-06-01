package com.fukang.knowledge.agent.api.servicedesk.dto;

import com.fukang.knowledge.agent.domain.agent.model.AgentRunEvent;

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
