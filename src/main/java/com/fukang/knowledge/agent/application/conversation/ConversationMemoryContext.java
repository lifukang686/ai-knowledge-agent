package com.fukang.knowledge.agent.application.conversation;

import com.fukang.knowledge.agent.infrastructure.persistence.entity.ConversationMessageDO;

import java.util.List;

/**
 * 一次 RAG 问答可用的会话记忆上下文。
 */
public record ConversationMemoryContext(
        Long conversationId,
        String summary,
        List<ConversationMessageDO> recentMessages,
        String rewriteHistory,
        String answerHistory
) {
    public boolean hasMemory() {
        return (summary != null && !summary.isBlank())
                || (recentMessages != null && !recentMessages.isEmpty());
    }
}
