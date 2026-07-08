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
    /**
     * Agent 返回给用户的回答内容。
     */
    private String answer;

    /**
     * 识别出的服务台意图。
     */
    private String intent;

    /**
     * 当前问题所属服务类型。
     */
    private String serviceType;

    /**
     * 本次运行状态。
     */
    private String status;

    /**
     * 服务台运行记录 ID。
     */
    private Long runId;

    /**
     * 已创建或关联的工单 ID。
     */
    private Long ticketId;

    /**
     * 已创建或关联的工单编号。
     */
    private String ticketNo;

    /**
     * 关联会话 ID。
     */
    private Long conversationId;

    /**
     * 是否需要用户确认后再创建工单。
     */
    private Boolean approvalRequired;

    /**
     * 待用户确认的草稿工单。
     */
    private ServiceTicketResp pendingTicket;

    /**
     * Agent 执行过程事件。
     */
    private List<AgentRunEvent> events;

    /**
     * 当前运行是否已提交反馈。
     */
    private Boolean feedbackSubmitted;

}
