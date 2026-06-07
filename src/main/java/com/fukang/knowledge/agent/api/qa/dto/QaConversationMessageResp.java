package com.fukang.knowledge.agent.api.qa.dto;

import java.time.LocalDateTime;

/**
 * QA 会话消息响应。
 */
public record QaConversationMessageResp(
        Long id,
        Long conversationId,
        String role,
        String content,
        String rewrittenQuery,
        String status,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {}
