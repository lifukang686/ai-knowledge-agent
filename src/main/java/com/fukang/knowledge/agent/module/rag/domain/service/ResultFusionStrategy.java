package com.fukang.knowledge.agent.module.rag.domain.service;

import com.fukang.knowledge.agent.module.rag.domain.model.SearchResult;

import java.util.List;

/**
 * 多路检索结果融合策略端口。
 */
public interface ResultFusionStrategy {

    /** 将向量检索和全文检索候选按 RRF 等策略融合。 */
    List<SearchResult> fuse(List<SearchResult> vectorResults, List<SearchResult> bm25Results, int topK, int rrfK);
}
