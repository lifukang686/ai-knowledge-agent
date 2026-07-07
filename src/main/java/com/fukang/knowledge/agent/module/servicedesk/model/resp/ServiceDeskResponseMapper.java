package com.fukang.knowledge.agent.module.servicedesk.model.resp;

import com.fukang.knowledge.agent.module.servicedesk.model.vo.ServiceDeskAnswerResult;
import com.fukang.knowledge.agent.module.servicedesk.model.vo.ServiceDeskFeedbackResult;
import com.fukang.knowledge.agent.module.servicedesk.model.vo.ServiceTicketEventResult;
import com.fukang.knowledge.agent.module.servicedesk.model.vo.ServiceTicketResult;

import java.util.List;

/**
 * 服务台响应转换器。
 * <p>集中处理 VO 到 Controller 响应对象的字段映射，避免转换逻辑散落在入口层。</p>
 */
public final class ServiceDeskResponseMapper {

    private ServiceDeskResponseMapper() {
    }

    /**
     * 转换服务台 Agent 最终回答。
     */
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

    /**
     * 转换工单响应。
     */
    public static ServiceTicketResp toTicketResp(ServiceTicketResult ticket) {
        List<ServiceTicketEventResp> events = ticket.getEvents() != null
                ? ticket.getEvents().stream().map(ServiceDeskResponseMapper::toTicketEventResp).toList()
                : List.of();
        return new ServiceTicketResp(ticket.getId(), ticket.getTicketNo(), ticket.getServiceType(), ticket.getCategory(),
                ticket.getPriority(), ticket.getStatus(), ticket.getTitle(), ticket.getDescription(), ticket.getAgentSummary(),
                ticket.getCreatorId(), ticket.getAssigneeId(), ticket.getSourceRunId(), ticket.getSourceConversationId(),
                events,
                ticket.getEventCount(), ticket.getCreateTime(), ticket.getUpdateTime());
    }

    /**
     * 转换工单事件响应。
     */
    private static ServiceTicketEventResp toTicketEventResp(ServiceTicketEventResult event) {
        return new ServiceTicketEventResp(event.getId(), event.getTicketId(), event.getEventType(), event.getFromStatus(),
                event.getToStatus(), event.getOperatorId(), event.getMessage(), event.getPayload(), event.getCreateTime());
    }

    /**
     * 转换服务台反馈响应。
     */
    public static ServiceDeskFeedbackResp toFeedbackResp(ServiceDeskFeedbackResult feedback) {
        return new ServiceDeskFeedbackResp(feedback.getId(), feedback.getRunId(), feedback.getTicketId(), feedback.getResolved(),
                feedback.getComment(), feedback.getUserId(), feedback.getCreateTime());
    }
}
