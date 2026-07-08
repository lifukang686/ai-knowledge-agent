package com.fukang.knowledge.agent.module.rag.model.bo;

import com.fukang.knowledge.agent.module.conversation.service.ConversationMemoryContext;
import com.fukang.knowledge.agent.module.memory.service.UserMemoryContext;
import com.fukang.knowledge.agent.module.rag.model.vo.SearchResult;
import com.fukang.knowledge.agent.module.rag.service.intent.QaIntent;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 流式 RAG 处理上下文。
 */
@Getter
public class RagStreamContext {

    /**
     * 用户原始问题。
     */
    private final String question;

    /**
     * 知识库 ID。
     */
    private final Long knowledgeBaseId;

    /**
     * 会话记忆上下文。
     */
    private final ConversationMemoryContext memory;

    /**
     * 用户长期记忆上下文。
     */
    private final UserMemoryContext userMemory;

    /**
     * 问题意图。
     */
    @Setter
    private QaIntent intent;

    /**
     * 改写后的检索查询。
     */
    @Setter
    private String rewrittenQuery;

    /**
     * 原始召回结果。
     */
    @Setter
    private List<SearchResult> retrieved = List.of();

    /**
     * 重排后的结果。
     */
    @Setter
    private List<SearchResult> reranked = List.of();

    public RagStreamContext(String question,
                            Long knowledgeBaseId,
                            ConversationMemoryContext memory,
                            UserMemoryContext userMemory) {
        this.question = question;
        this.knowledgeBaseId = knowledgeBaseId;
        this.memory = memory;
        this.userMemory = userMemory;
    }
}
