package com.fukang.knowledge.agent.application.servicedesk.result;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 服务台工单结果。
 */
public record ServiceTicketResult(
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
        List<ServiceTicketEventResult> events,
        Long eventCount,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
