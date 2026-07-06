package com.fukang.knowledge.agent.module.servicedesk.model.resp;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 服务台用户反馈响应。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceDeskFeedbackResp {
    private Long id;

    private Long runId;

    private Long ticketId;

    private Boolean resolved;

    private String comment;

    private Long userId;

    private LocalDateTime createTime;

}
