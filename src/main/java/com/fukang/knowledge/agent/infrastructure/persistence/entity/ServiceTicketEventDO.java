package com.fukang.knowledge.agent.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 服务台工单状态和治理事件实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "service_ticket_event")
@TableName("service_ticket_event")
public class ServiceTicketEventDO extends BaseEntity {

    /** 关联工单 ID。 */
    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;

    /** 事件类型，如 DRAFT_CREATED、CONFIRMED。 */
    @Column(name = "event_type", nullable = false, length = 40)
    private String eventType;

    /** 事件发生前的工单状态。 */
    @Column(name = "from_status", length = 20)
    private String fromStatus;

    /** 事件发生后的工单状态。 */
    @Column(name = "to_status", length = 20)
    private String toStatus;

    /** 操作人用户 ID。 */
    @Column(name = "operator_id")
    private Long operatorId;

    /** 面向前端时间线展示的简短说明。 */
    @Column(name = "message", length = 500)
    private String message;

    /** 扩展载荷 JSON。 */
    @Column(name = "payload", columnDefinition = "text")
    private String payload;
}
