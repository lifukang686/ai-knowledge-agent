package com.fukang.knowledge.agent.domain.servicedesk;

import java.util.Locale;

/**
 * 服务台工单优先级。
 */
public enum TicketPriority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT;

    public static TicketPriority from(String value) {
        if (value == null || value.isBlank()) {
            return MEDIUM;
        }
        try {
            return TicketPriority.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return MEDIUM;
        }
    }
}
