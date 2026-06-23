package com.fukang.knowledge.agent.domain.servicedesk;

import java.util.Locale;

/**
 * 服务台 Agent 可识别的业务意图。
 */
public enum ServiceDeskIntent {
    /**
     * 知识库问答。
     */
    KNOWLEDGE_QA("knowledge_qa"),
    /**
     * 创建工单。
     */
    CREATE_TICKET("create_ticket"),
    /**
     * 查询工单。
     */
    QUERY_TICKET("query_ticket"),
    /**
     * 收集补充信息。
     */
    COLLECT_INFO("collect_info"),
    /**
     * 转人工介入。
     */
    HANDOFF_HUMAN("handoff_human"),
    /**
     * 文档总结。
     */
    SUMMARIZE_DOCUMENT("summarize_document");

    private final String code;

    ServiceDeskIntent(String code) {
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
