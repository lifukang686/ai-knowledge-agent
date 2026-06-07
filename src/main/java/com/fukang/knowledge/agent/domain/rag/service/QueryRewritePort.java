package com.fukang.knowledge.agent.domain.rag.service;

/**
 * 查询改写端口，用于提升多轮问答和短问题的可检索性。
 */
public interface QueryRewritePort {

    /** 默认改写策略。 */
    String rewrite(String originalQuery);

    /** 结合会话记忆和用户记忆改写查询。 */
    String rewriteWithHistory(String originalQuery, String conversationSummary, String conversationHistory, String userMemory);

    /** 抽象式改写，适合补全省略语义。 */
    String rewriteAbstractive(String originalQuery);

    /** 抽取式改写，适合保留关键词。 */
    String rewriteExtractive(String originalQuery);

    /** 混合改写，兼顾语义补全和关键词稳定性。 */
    String rewriteHybrid(String originalQuery);
}
