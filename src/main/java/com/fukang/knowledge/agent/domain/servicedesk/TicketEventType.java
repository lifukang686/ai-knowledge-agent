package com.fukang.knowledge.agent.domain.servicedesk;

/**
 * 服务台工单事件类型。
 */
public enum TicketEventType {
    /**
     * 草稿已创建。
     */
    DRAFT_CREATED,
    /**
     * 用户已确认。
     */
    CONFIRMED,
    /**
     * 状态已变更。
     */
    STATUS_CHANGED,
    /**
     * 已请求人工介入。
     */
    HANDOFF_REQUESTED
}
