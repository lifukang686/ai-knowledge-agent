package com.fukang.knowledge.agent.application.conversation;

import com.fukang.knowledge.agent.infrastructure.persistence.entity.ConversationMessageDO;

import java.util.List;

/**
 * 一次 RAG 问答可用的会话记忆上下文。
 */
public record ConversationMemoryContext(
        Long conversationId,
        String summary,//长期压缩记忆
        List<ConversationMessageDO> recentMessages,//最进原始消息
        String rewriteHistory,//最近问题
        String answerHistory//最近对话
) {
    public boolean hasMemory() {
        return (summary != null && !summary.isBlank())
                || (recentMessages != null && !recentMessages.isEmpty());
    }
}
