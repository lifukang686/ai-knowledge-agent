package com.fukang.knowledge.agent.domain.rag.service;

import com.fukang.knowledge.agent.domain.rag.model.SearchResult;

import java.util.List;

/**
 * 检索结果重排端口。
 */
public interface Reranker {

    /** 按用户问题对候选片段重新排序。 */
    List<SearchResult> rerank(List<SearchResult> candidates, String query);
}
