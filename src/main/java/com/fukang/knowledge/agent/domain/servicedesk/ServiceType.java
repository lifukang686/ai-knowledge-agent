package com.fukang.knowledge.agent.domain.servicedesk;

import java.util.Locale;

/**
 * 服务台业务类型。
 */
public enum ServiceType {
    AUTO,
    IT,
    HR;

    public static ServiceType from(String value) {
        if (value == null || value.isBlank()) {
            return AUTO;
        }
        try {
            return ServiceType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return AUTO;
        }
    }
}
