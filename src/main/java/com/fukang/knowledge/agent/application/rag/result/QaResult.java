package com.fukang.knowledge.agent.application.rag.result;

public record QaResult(
        String answer,
        String rewrittenQuery,
        String status
) {}
