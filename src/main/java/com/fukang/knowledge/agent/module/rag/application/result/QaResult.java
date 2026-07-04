package com.fukang.knowledge.agent.module.rag.application.result;

/**
 * RAG 问答结果。
 */
public record QaResult(
        String answer,
        String rewrittenQuery,
        String status,
        Long conversationId
) {
    public QaResult(String answer, String rewrittenQuery, String status) {
        this(answer, rewrittenQuery, status, null);
    }
}
