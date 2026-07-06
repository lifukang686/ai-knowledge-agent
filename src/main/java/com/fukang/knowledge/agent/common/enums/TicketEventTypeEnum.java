package com.fukang.knowledge.agent.common.enums;

/**
 * 服务台工单事件类型。
 */
public enum TicketEventTypeEnum {
    /**
     * 草稿已创建。
     */
    DRAFT_CREATED,
    CONFIRMED,
    STATUS_CHANGED,
    HANDOFF_REQUESTED
}
