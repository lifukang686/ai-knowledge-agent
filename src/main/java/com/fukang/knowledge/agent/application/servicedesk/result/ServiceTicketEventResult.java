package com.fukang.knowledge.agent.application.servicedesk.result;

import java.time.LocalDateTime;

/**
 * 服务台工单事件结果。
 */
public record ServiceTicketEventResult(
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
