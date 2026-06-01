package com.fukang.knowledge.agent.api.servicedesk.dto;

import java.time.LocalDateTime;

/**
 * 服务台工单事件响应。
 */
public record ServiceTicketEventResp(
        Long id,
        Long ticketId,
        String eventType,
        String fromStatus,
        String toStatus,
        Long operatorId,
        String message,
        String payload,
        LocalDateTime createTime
) {
}
