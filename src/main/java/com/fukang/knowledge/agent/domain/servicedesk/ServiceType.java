package com.fukang.knowledge.agent.domain.servicedesk;

import java.util.Locale;

/**
 * 服务台业务类型。
 */
public enum ServiceType {
    /**
     * 自动识别服务类型。
     */
    AUTO,
    /**
     * IT 服务台。
     */
    IT,
    /**
     * HR 服务台。
     */
    HR;

    /**
     * 解析服务类型，非法值回退 AUTO。
     */
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
