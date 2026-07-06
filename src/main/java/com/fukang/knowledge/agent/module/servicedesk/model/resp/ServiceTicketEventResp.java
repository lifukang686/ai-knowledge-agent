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
