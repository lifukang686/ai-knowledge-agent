package com.fukang.knowledge.agent.module.servicedesk.model.resp;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 服务台工单事件响应。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceTicketEventResp {
    /**
     * 工单事件 ID。
     */
    private Long id;

    /**
     * 关联工单 ID。
     */
    private Long ticketId;

    /**
     * 工单事件类型。
     */
    private String eventType;

    /**
     * 事件发生前的工单状态。
     */
    private String fromStatus;

    /**
     * 事件发生后的工单状态。
     */
    private String toStatus;

    /**
     * 操作人用户 ID。
     */
    private Long operatorId;

    /**
     * 事件展示文案。
     */
    private String message;

    /**
     * 事件扩展载荷 JSON。
     */
    private String payload;

    /**
     * 事件创建时间。
     */
    private LocalDateTime createTime;

}
