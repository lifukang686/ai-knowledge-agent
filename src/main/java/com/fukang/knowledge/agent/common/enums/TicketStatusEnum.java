package com.fukang.knowledge.agent.common.enums;

import java.util.Locale;

/**
 * 服务台工单状态。
 */
public enum TicketStatusEnum {
    /**
     * 草稿。
     */
    DRAFT,
    OPEN,
    PROCESSING,
    RESOLVED,
    CLOSED;

    /**
     * 解析工单状态，非法值返回 null。
     */
    public static TicketStatusEnum from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return TicketStatusEnum.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
