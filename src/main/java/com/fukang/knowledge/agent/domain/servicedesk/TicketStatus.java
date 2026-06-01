package com.fukang.knowledge.agent.domain.servicedesk;

import java.util.Locale;

/**
 * 服务台工单状态。
 */
public enum TicketStatus {
    DRAFT,
    OPEN,
    PROCESSING,
    RESOLVED,
    CLOSED;

    public static TicketStatus from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return TicketStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
