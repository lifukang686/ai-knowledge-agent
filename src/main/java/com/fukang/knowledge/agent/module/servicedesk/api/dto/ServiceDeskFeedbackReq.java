package com.fukang.knowledge.agent.module.servicedesk.api.dto;

/**
 * 服务台用户反馈请求。
 */
public record ServiceDeskFeedbackReq(
        Boolean resolved,
        String comment
) {
}
