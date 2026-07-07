package com.fukang.knowledge.agent.module.rag.service.intent;

/**
 * QA 请求意图分类。
 */
public enum QaIntent {
    /** 需要检索知识库后回答。 */
    RAG_QA,
    /** 普通闲聊或助手能力说明，无需检索知识库。 */
    DIRECT_CHAT,
    /** 用户主动提供个人信息，需要写入用户记忆。 */
    MEMORY_UPDATE,
    /** 用户围绕已记忆信息发起对话，无需检索知识库。 */
    MEMORY_CHAT,
    /** 规则无法判断，交由 LLM 或默认路径处理。 */
    UNKNOWN;

    /**
     * 是否无需检索知识库。
     */
    public boolean bypassRetrieval() {
        return this == DIRECT_CHAT || this == MEMORY_UPDATE || this == MEMORY_CHAT;
    }
}
