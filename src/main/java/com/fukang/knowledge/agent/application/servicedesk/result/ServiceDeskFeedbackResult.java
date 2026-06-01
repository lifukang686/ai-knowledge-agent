package com.fukang.knowledge.agent.application.servicedesk.result;

import java.time.LocalDateTime;

/**
 * 服务台用户反馈结果。
 */
public record ServiceDeskFeedbackResult(
        Long id,
        Long runId,
        Long ticketId,
        Boolean resolved,
        String comment,
        Long userId,
        LocalDateTime createTime
) {
}
