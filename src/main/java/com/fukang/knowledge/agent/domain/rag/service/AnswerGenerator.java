package com.fukang.knowledge.agent.domain.rag.service;

import com.fukang.knowledge.agent.domain.rag.model.SearchResult;

import java.util.List;

/**
 * RAG 答案生成领域端口。
 */
public interface AnswerGenerator {

    /** 基于检索结果和查询生成回答。 */
    String generateAnswer(List<SearchResult> results, String query);

    /** 基于检索结果、查询和会话记忆生成回答。 */
    String generateAnswer(List<SearchResult> results, String query, String conversationMemory);
}
