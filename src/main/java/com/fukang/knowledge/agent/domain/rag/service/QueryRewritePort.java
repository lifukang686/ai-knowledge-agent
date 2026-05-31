package com.fukang.knowledge.agent.domain.rag.service;

public interface QueryRewritePort {
    String rewrite(String originalQuery);

    String rewriteWithHistory(String originalQuery, String conversationSummary, String conversationHistory);

    String rewriteAbstractive(String originalQuery);

    String rewriteExtractive(String originalQuery);

    String rewriteHybrid(String originalQuery);
}
