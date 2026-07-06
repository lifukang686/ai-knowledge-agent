package com.fukang.knowledge.agent.module.servicedesk.model.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 服务台用户反馈请求。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceDeskFeedbackReq {
    private Boolean resolved;

    private String comment;

}
