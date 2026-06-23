package com.fukang.knowledge.agent.domain.servicedesk;

import java.util.Locale;

/**
 * 服务台工单状态。
 */
public enum TicketStatus {
    /**
     * 草稿。
     */
    DRAFT,
    /**
     * 已打开。
     */
    OPEN,
    /**
     * 处理中。
     */
    PROCESSING,
    /**
     * 已解决。
     */
    RESOLVED,
    /**
     * 已关闭。
     */
    CLOSED;

    /**
     * 解析工单状态，非法值返回 null。
     */
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
