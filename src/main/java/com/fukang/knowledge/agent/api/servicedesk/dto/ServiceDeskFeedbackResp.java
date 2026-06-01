package com.fukang.knowledge.agent.api.servicedesk.dto;

import java.time.LocalDateTime;

/**
 * 服务台用户反馈响应。
 */
public record ServiceDeskFeedbackResp(
        Long id,
        Long runId,
        Long ticketId,
        Boolean resolved,
        String comment,
        Long userId,
        LocalDateTime createTime
) {
}
