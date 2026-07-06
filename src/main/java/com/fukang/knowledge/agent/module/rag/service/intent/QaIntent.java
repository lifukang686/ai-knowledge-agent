package com.fukang.knowledge.agent.module.rag.service.intent;

/**
 * QA 请求意图分类。
 */
public enum QaIntent {
    /** 需要检索知识库后回答。 */
    RAG_QA,
    DIRECT_CHAT,
    MEMORY_UPDATE,
    MEMORY_CHAT,
    UNKNOWN;

    /**
     * 是否无需检索知识库。
     */
    public boolean bypassRetrieval() {
        return this == DIRECT_CHAT || this == MEMORY_UPDATE || this == MEMORY_CHAT;
    }
}
