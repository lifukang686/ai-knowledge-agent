package com.fukang.knowledge.agent.api.servicedesk.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 服务台工单响应。
 */
public record ServiceTicketResp(
        Long id,
        String ticketNo,
        String serviceType,
        String category,
        String priority,
        String status,
        String title,
        String description,
        String agentSummary,
        Long creatorId,
        Long assigneeId,
        Long sourceRunId,
        Long sourceConversationId,
        List<ServiceTicketEventResp> events,
        Long eventCount,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
