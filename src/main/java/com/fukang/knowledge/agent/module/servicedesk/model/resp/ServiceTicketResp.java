package com.fukang.knowledge.agent.module.servicedesk.model.resp;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 服务台工单响应。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceTicketResp {
    /**
     * 工单 ID。
     */
    private Long id;

    /**
     * 面向用户展示的工单编号。
     */
    private String ticketNo;

    /**
     * 工单所属服务类型。
     */
    private String serviceType;

    /**
     * 工单分类。
     */
    private String category;

    /**
     * 工单优先级。
     */
    private String priority;

    /**
     * 工单当前状态。
     */
    private String status;

    /**
     * 工单标题。
     */
    private String title;

    /**
     * 工单问题描述。
     */
    private String description;

    /**
     * Agent 生成的处理摘要。
     */
    private String agentSummary;

    /**
     * 创建人用户 ID。
     */
    private Long creatorId;

    /**
     * 当前负责人用户 ID。
     */
    private Long assigneeId;

    /**
     * 来源服务台运行记录 ID。
     */
    private Long sourceRunId;

    /**
     * 来源会话 ID。
     */
    private Long sourceConversationId;

    /**
     * 工单事件列表。
     */
    private List<ServiceTicketEventResp> events;

    /**
     * 工单事件总数。
     */
    private Long eventCount;

    /**
     * 工单创建时间。
     */
    private LocalDateTime createTime;

    /**
     * 工单更新时间。
     */
    private LocalDateTime updateTime;

}
