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
    /**
     * 用户反馈问题是否已经解决。
     */
    private Boolean resolved;

    /**
     * 用户反馈备注。
     */
    private String comment;

}
