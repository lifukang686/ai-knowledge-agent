package com.fukang.knowledge.agent.api.qa.dto;

import java.time.LocalDateTime;

/**
 * QA 会话列表响应。
 */
public record QaConversationResp(
        Long id,
        Long knowledgeBaseId,
        String title,
        String status,
        long messageCount,
        LocalDateTime lastMessageAt,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {}
