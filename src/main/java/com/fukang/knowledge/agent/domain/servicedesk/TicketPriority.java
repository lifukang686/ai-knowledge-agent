package com.fukang.knowledge.agent.domain.servicedesk;

import java.util.Locale;

/**
 * 服务台工单优先级。
 */
public enum TicketPriority {
    /**
     * 低优先级。
     */
    LOW,
    /**
     * 中优先级。
     */
    MEDIUM,
    /**
     * 高优先级。
     */
    HIGH,
    /**
     * 紧急优先级。
     */
    URGENT;

    /**
     * 解析优先级，非法值回退 MEDIUM。
     */
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
