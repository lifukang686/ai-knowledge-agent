package com.fukang.knowledge.agent.domain.rag.service;

import com.fukang.knowledge.agent.domain.rag.model.SearchResult;

import java.util.List;

/**
 * 知识库检索策略端口。
 */
public interface RetrievalStrategy {

    /** 按查询文本召回指定知识库中的候选片段。 */
    List<SearchResult> retrieve(String queryText, Long knowledgeBaseId, int topK, double threshold);
}
