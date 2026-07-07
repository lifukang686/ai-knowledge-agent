package com.fukang.knowledge.agent.module.conversation.service;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 一次 RAG 问答可用的会话记忆上下文。
 * <p>只暴露下游 Prompt 组装需要的摘要和历史文本，避免把消息实体泄漏到 RAG 编排层。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMemoryContext {
    private Long conversationId;

    private String summary;

    private String rewriteHistory;

    private String answerHistory;
}
