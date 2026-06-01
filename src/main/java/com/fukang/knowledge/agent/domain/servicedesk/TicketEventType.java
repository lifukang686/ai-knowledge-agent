package com.fukang.knowledge.agent.domain.servicedesk;

/**
 * 服务台工单事件类型。
 */
public enum TicketEventType {
    DRAFT_CREATED,
    CONFIRMED,
    STATUS_CHANGED,
    HANDOFF_REQUESTED
}
