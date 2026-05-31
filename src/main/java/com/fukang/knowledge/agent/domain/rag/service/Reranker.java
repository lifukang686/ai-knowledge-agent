package com.fukang.knowledge.agent.domain.rag.service;

import com.fukang.knowledge.agent.domain.rag.model.SearchResult;

import java.util.List;

public interface Reranker {
    List<SearchResult> rerank(List<SearchResult> candidates, String query);
}
