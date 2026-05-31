package com.fukang.knowledge.agent.application.rag.result;

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
