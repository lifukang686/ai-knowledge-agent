package com.fukang.knowledge.agent.module.servicedesk.model.resp;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fukang.knowledge.agent.module.agent.model.vo.AgentRunEvent;

import java.util.List;

/**
 * 服务台问答响应。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceDeskAnswerResp {
    private String answer;

    private String intent;

    private String serviceType;

    private String status;

    private Long runId;

    private Long ticketId;

    private String ticketNo;

    private Long conversationId;

    private Boolean approvalRequired;

    private ServiceTicketResp pendingTicket;

    private List<AgentRunEvent> events;

    private Boolean feedbackSubmitted;

}
