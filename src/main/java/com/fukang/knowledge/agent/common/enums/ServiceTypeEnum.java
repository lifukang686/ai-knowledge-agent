package com.fukang.knowledge.agent.common.enums;

import java.util.Locale;

/**
 * 服务台业务类型。
 */
public enum ServiceTypeEnum {
    /**
     * 自动识别服务类型。
     */
    AUTO,
    IT,
    HR;

    /**
     * 解析服务类型，非法值回退 AUTO。
     */
    public static ServiceTypeEnum from(String value) {
        if (value == null || value.isBlank()) {
            return AUTO;
        }
        try {
            return ServiceTypeEnum.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return AUTO;
        }
    }
}
