package com.fukang.knowledge.agent.common.enums;

import java.util.Locale;

/**
 * 服务台 Agent 可识别的业务意图。
 */
public enum ServiceDeskIntentEnum {
    /**
     * 知识库问答。
     */
    KNOWLEDGE_QA("knowledge_qa"),
    CREATE_TICKET("create_ticket"),
    QUERY_TICKET("query_ticket"),
    COLLECT_INFO("collect_info"),
    HANDOFF_HUMAN("handoff_human"),
    SUMMARIZE_DOCUMENT("summarize_document");

    private final String code;

    ServiceDeskIntentEnum(String code) {
        this.code = code;
    }

    /**
     * 返回意图编码。
     */
    public String code() {
        return code;
    }

    /**
     * 按编码或枚举名解析意图。
     */
    public static ServiceDeskIntentEnum from(String value) {
        if (value == null || value.isBlank()) {
            return KNOWLEDGE_QA;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (ServiceDeskIntentEnum intent : values()) {
            if (intent.code.equals(normalized) || intent.name().equalsIgnoreCase(normalized)) {
                return intent;
            }
        }
        return KNOWLEDGE_QA;
    }
}
