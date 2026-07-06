package com.fukang.knowledge.agent.module.servicedesk.model.resp;

import com.fukang.knowledge.agent.module.servicedesk.model.vo.ServiceDeskAnswerResult;
import com.fukang.knowledge.agent.module.servicedesk.model.vo.ServiceDeskFeedbackResult;
import com.fukang.knowledge.agent.module.servicedesk.model.vo.ServiceTicketEventResult;
import com.fukang.knowledge.agent.module.servicedesk.model.vo.ServiceTicketResult;

public final class ServiceDeskResponseMapper {

    private ServiceDeskResponseMapper() {
    }

    public static ServiceDeskAnswerResp toAnswerResp(ServiceDeskAnswerResult result) {
        return new ServiceDeskAnswerResp(
                result.getAnswer() != null ? result.getAnswer() : "",
                result.getIntent() != null ? result.getIntent() : "",
                result.getServiceType() != null ? result.getServiceType() : "",
                result.getStatus() != null ? result.getStatus() : "success",
                result.getRunId(),
                result.getTicketId(),
                result.getTicketNo(),
                result.getConversationId(),
                result.getApprovalRequired(),
                result.getPendingTicket() != null ? toTicketResp(result.getPendingTicket()) : null,
                result.getEvents(),
                result.getFeedbackSubmitted());
    }

    public static ServiceTicketResp toTicketResp(ServiceTicketResult ticket) {
        return new ServiceTicketResp(ticket.getId(), ticket.getTicketNo(), ticket.getServiceType(), ticket.getCategory(),
                ticket.getPriority(), ticket.getStatus(), ticket.getTitle(), ticket.getDescription(), ticket.getAgentSummary(),
                ticket.getCreatorId(), ticket.getAssigneeId(), ticket.getSourceRunId(), ticket.getSourceConversationId(),
                ticket.getEvents().stream().map(ServiceDeskResponseMapper::toTicketEventResp).toList(),
                ticket.getEventCount(), ticket.getCreateTime(), ticket.getUpdateTime());
    }

    public static ServiceTicketEventResp toTicketEventResp(ServiceTicketEventResult event) {
        return new ServiceTicketEventResp(event.getId(), event.getTicketId(), event.getEventType(), event.getFromStatus(),
                event.getToStatus(), event.getOperatorId(), event.getMessage(), event.getPayload(), event.getCreateTime());
    }

    public static ServiceDeskFeedbackResp toFeedbackResp(ServiceDeskFeedbackResult feedback) {
        return new ServiceDeskFeedbackResp(feedback.getId(), feedback.getRunId(), feedback.getTicketId(), feedback.getResolved(),
                feedback.getComment(), feedback.getUserId(), feedback.getCreateTime());
    }
}
