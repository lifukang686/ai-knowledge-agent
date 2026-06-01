package com.fukang.knowledge.agent.api.servicedesk.dto;

/**
 * 服务台用户反馈请求。
 */
public record ServiceDeskFeedbackReq(
        Boolean resolved,
        String comment
) {
}
