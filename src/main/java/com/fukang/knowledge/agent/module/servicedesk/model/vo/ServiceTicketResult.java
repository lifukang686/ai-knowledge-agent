package com.fukang.knowledge.agent.module.servicedesk.model.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 服务台工单结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceTicketResult {
    private Long id;

    private String ticketNo;

    private String serviceType;

    private String category;

    private String priority;

    private String status;

    private String title;

    private String description;

    private String agentSummary;

    private Long creatorId;

    private Long assigneeId;

    private Long sourceRunId;

    private Long sourceConversationId;

    private List<ServiceTicketEventResult> events;

    private Long eventCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

}
