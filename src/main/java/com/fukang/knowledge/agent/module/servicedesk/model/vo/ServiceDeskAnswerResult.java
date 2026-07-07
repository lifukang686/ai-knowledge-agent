package com.fukang.knowledge.agent.module.servicedesk.model.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fukang.knowledge.agent.module.agent.model.vo.AgentRunEvent;

import java.util.List;

/**
 * 服务台问答结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceDeskAnswerResult {
    private String answer;

    private String intent;

    private String serviceType;

    private String status;

    private Long runId;

    private Long ticketId;

    private String ticketNo;

    private Long conversationId;

    private Boolean approvalRequired;

    private ServiceTicketResult pendingTicket;

    private List<AgentRunEvent> events;

    private Boolean feedbackSubmitted;

    public ServiceDeskAnswerResult withEvents(List<AgentRunEvent> newEvents) {
        return new ServiceDeskAnswerResult(answer, intent, serviceType, status, runId, ticketId, ticketNo,
                conversationId, approvalRequired, pendingTicket, newEvents, feedbackSubmitted);
    }

}
