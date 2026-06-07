package com.fukang.knowledge.agent.application.conversation.result;

import java.time.LocalDateTime;

/**
 * QA 会话列表项。
 */
public record ConversationListItemResult(
        Long id,
        Long knowledgeBaseId,
        String title,
        String status,
        long messageCount,
        LocalDateTime lastMessageAt,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {}
