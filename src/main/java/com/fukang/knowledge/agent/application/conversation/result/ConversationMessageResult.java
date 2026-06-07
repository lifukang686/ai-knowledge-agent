package com.fukang.knowledge.agent.application.conversation.result;

import java.time.LocalDateTime;

/**
 * QA 会话消息。
 */
public record ConversationMessageResult(
        Long id,
        Long conversationId,
        String role,
        String content,
        String rewrittenQuery,
        String status,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {}
