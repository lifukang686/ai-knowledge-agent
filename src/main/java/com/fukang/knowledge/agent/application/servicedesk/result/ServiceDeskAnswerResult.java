package com.fukang.knowledge.agent.application.servicedesk.result;

import com.fukang.knowledge.agent.domain.agent.model.AgentRunEvent;

import java.util.List;

/**
 * 服务台问答结果。
 */
public record ServiceDeskAnswerResult(
        String answer,
        String intent,
        String serviceType,
        String status,
        Long runId,
        Long ticketId,
        String ticketNo,
        Long conversationId,
        Boolean approvalRequired,
        ServiceTicketResult pendingTicket,
        List<AgentRunEvent> events,
        Boolean feedbackSubmitted
) {
    public ServiceDeskAnswerResult(String answer, String intent, String serviceType, String status,
                                   Long runId, Long ticketId, String ticketNo, Long conversationId) {
        this(answer, intent, serviceType, status, runId, ticketId, ticketNo, conversationId,
                false, null, List.of(), false);
    }

    public ServiceDeskAnswerResult withEvents(List<AgentRunEvent> newEvents) {
        return new ServiceDeskAnswerResult(answer, intent, serviceType, status, runId, ticketId, ticketNo,
                conversationId, approvalRequired, pendingTicket, newEvents, feedbackSubmitted);
    }

    public ServiceDeskAnswerResult withFeedbackSubmitted(Boolean submitted) {
        return new ServiceDeskAnswerResult(answer, intent, serviceType, status, runId, ticketId, ticketNo,
                conversationId, approvalRequired, pendingTicket, events, submitted);
    }
}
