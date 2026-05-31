package com.fukang.knowledge.agent.domain.rag.service;

import com.fukang.knowledge.agent.domain.rag.model.SearchResult;

import java.util.List;

public interface RetrievalStrategy {
    List<SearchResult> retrieve(String queryText, Long knowledgeBaseId, int topK, double threshold);
}
