package com.fukang.knowledge.agent.domain.servicedesk;

import java.util.Locale;

/**
 * 服务台 Agent 可识别的业务意图。
 */
public enum ServiceDeskIntent {
    KNOWLEDGE_QA("knowledge_qa"),
    CREATE_TICKET("create_ticket"),
    QUERY_TICKET("query_ticket"),
    COLLECT_INFO("collect_info"),
    HANDOFF_HUMAN("handoff_human"),
    SUMMARIZE_DOCUMENT("summarize_document");

    private final String code;

    ServiceDeskIntent(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static ServiceDeskIntent from(String value) {
        if (value == null || value.isBlank()) {
            return KNOWLEDGE_QA;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (ServiceDeskIntent intent : values()) {
            if (intent.code.equals(normalized) || intent.name().equalsIgnoreCase(normalized)) {
                return intent;
            }
        }
        return KNOWLEDGE_QA;
    }
}
