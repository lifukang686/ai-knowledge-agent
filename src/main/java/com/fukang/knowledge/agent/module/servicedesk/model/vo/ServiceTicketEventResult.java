package com.fukang.knowledge.agent.module.servicedesk.model.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 服务台工单事件结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceTicketEventResult {
    private Long id;

    private Long ticketId;

    private String eventType;

    private String fromStatus;

    private String toStatus;

    private Long operatorId;

    private String message;

    private String payload;

    private LocalDateTime createTime;

}
