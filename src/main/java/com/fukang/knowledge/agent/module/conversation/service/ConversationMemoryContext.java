package com.fukang.knowledge.agent.module.conversation.service;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fukang.knowledge.agent.module.conversation.model.entity.ConversationMessageEntity;

import java.util.List;

/**
 * 一次 RAG 问答可用的会话记忆上下文。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMemoryContext {
    private Long conversationId;

    private String summary;

    private List<ConversationMessageEntity> recentMessages;

    private String rewriteHistory;

    private String answerHistory;

    public boolean hasMemory() {
        return (summary != null && !summary.isBlank())
                || (recentMessages != null && !recentMessages.isEmpty());
    }

}
