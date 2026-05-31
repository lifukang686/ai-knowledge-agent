package com.fukang.knowledge.agent.domain.rag.service;

import com.fukang.knowledge.agent.domain.rag.model.SearchResult;

import java.util.List;

public interface ResultFusionStrategy {
    List<SearchResult> fuse(List<SearchResult> vectorResults, List<SearchResult> bm25Results, int topK, int rrfK);
}
