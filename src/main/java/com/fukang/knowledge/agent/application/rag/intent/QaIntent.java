package com.fukang.knowledge.agent.application.rag.intent;

/**
 * QA 请求意图分类。
 */
public enum QaIntent {
    /** 需要检索知识库后回答。 */
    RAG_QA,
    /** 问候、感谢、助手身份、能力介绍等直接对话。 */
    DIRECT_CHAT,
    /** 用户提供姓名、偏好、身份等记忆信息。 */
    MEMORY_UPDATE,
    /** 用户同时提供记忆信息并发起闲聊。 */
    MEMORY_CHAT,
    /** 无法判断时按 RAG 问答处理。 */
    UNKNOWN;

    public boolean bypassRetrieval() {
        return this == DIRECT_CHAT || this == MEMORY_UPDATE || this == MEMORY_CHAT;
    }
}
