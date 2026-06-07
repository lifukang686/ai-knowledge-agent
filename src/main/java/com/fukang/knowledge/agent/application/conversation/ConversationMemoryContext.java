package com.fukang.knowledge.agent.application.conversation;

import com.fukang.knowledge.agent.infrastructure.persistence.entity.ConversationMessageDO;

import java.util.List;

/**
 * 一次 RAG 问答可用的会话记忆上下文。
 */
public record ConversationMemoryContext(
        Long conversationId,
        String summary,//[长期压缩记忆].用在查询改写，也用在回答生成/直接对话
        List<ConversationMessageDO> recentMessages,//最近 x 条，包含 user 和 assistant 原始消息
        String rewriteHistory,//[最近问题].来自 recentMessages，但过滤掉 assistant，只保留 user;用在查询改写
        String answerHistory//[最近对话].来自 recentMessages，保留 user + assistant;用在回答生成/直接对话
) {
    public boolean hasMemory() {
        return (summary != null && !summary.isBlank())
                || (recentMessages != null && !recentMessages.isEmpty());
    }
}
