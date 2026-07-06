package com.fukang.knowledge.agent.common.enums;

import java.util.Locale;

/**
 * 服务台工单优先级。
 */
public enum TicketPriorityEnum {
    /**
     * 低优先级。
     */
    LOW,
    MEDIUM,
    HIGH,
    URGENT;

    /**
     * 解析优先级，非法值回退 MEDIUM。
     */
    public static TicketPriorityEnum from(String value) {
        if (value == null || value.isBlank()) {
            return MEDIUM;
        }
        try {
            return TicketPriorityEnum.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return MEDIUM;
        }
    }
}
