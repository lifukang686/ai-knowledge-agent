package com.fukang.knowledge.agent.module.servicedesk.api;

import com.fukang.knowledge.agent.module.servicedesk.api.dto.ServiceDeskAnswerResp;
import com.fukang.knowledge.agent.module.servicedesk.api.dto.ServiceDeskFeedbackResp;
import com.fukang.knowledge.agent.module.servicedesk.api.dto.ServiceTicketEventResp;
import com.fukang.knowledge.agent.module.servicedesk.api.dto.ServiceTicketResp;
import com.fukang.knowledge.agent.module.servicedesk.application.result.ServiceDeskAnswerResult;
import com.fukang.knowledge.agent.module.servicedesk.application.result.ServiceDeskFeedbackResult;
import com.fukang.knowledge.agent.module.servicedesk.application.result.ServiceTicketEventResult;
import com.fukang.knowledge.agent.module.servicedesk.application.result.ServiceTicketResult;

final class ServiceDeskResponseMapper {

    private ServiceDeskResponseMapper() {
    }

    static ServiceDeskAnswerResp toAnswerResp(ServiceDeskAnswerResult result) {
        return new ServiceDeskAnswerResp(
                result.answer() != null ? result.answer() : "",
                result.intent() != null ? result.intent() : "",
                result.serviceType() != null ? result.serviceType() : "",
                result.status() != null ? result.status() : "success",
                result.runId(),
                result.ticketId(),
                result.ticketNo(),
                result.conversationId(),
                result.approvalRequired(),
                result.pendingTicket() != null ? toTicketResp(result.pendingTicket()) : null,
                result.events(),
                result.feedbackSubmitted());
    }

    static ServiceTicketResp toTicketResp(ServiceTicketResult ticket) {
        return new ServiceTicketResp(ticket.id(), ticket.ticketNo(), ticket.serviceType(), ticket.category(),
                ticket.priority(), ticket.status(), ticket.title(), ticket.description(), ticket.agentSummary(),
                ticket.creatorId(), ticket.assigneeId(), ticket.sourceRunId(), ticket.sourceConversationId(),
                ticket.events().stream().map(ServiceDeskResponseMapper::toTicketEventResp).toList(),
                ticket.eventCount(), ticket.createTime(), ticket.updateTime());
    }

    static ServiceTicketEventResp toTicketEventResp(ServiceTicketEventResult event) {
        return new ServiceTicketEventResp(event.id(), event.ticketId(), event.eventType(), event.fromStatus(),
                event.toStatus(), event.operatorId(), event.message(), event.payload(), event.createTime());
    }

    static ServiceDeskFeedbackResp toFeedbackResp(ServiceDeskFeedbackResult feedback) {
        return new ServiceDeskFeedbackResp(feedback.id(), feedback.runId(), feedback.ticketId(), feedback.resolved(),
                feedback.comment(), feedback.userId(), feedback.createTime());
    }
}
