package com.fukang.knowledge.agent.module.rag.service.impl;

import com.fukang.knowledge.agent.module.rag.model.vo.SearchResult;
import com.fukang.knowledge.agent.module.rag.service.FullTextSearchService;
import com.fukang.knowledge.agent.module.rag.service.ResultFusionStrategy;
import com.fukang.knowledge.agent.module.rag.service.RetrievalStrategy;
import com.fukang.knowledge.agent.module.rag.service.SemanticSearchService;
import com.fukang.knowledge.agent.infrastructure.config.RetrievalProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 混合检索服务
 * <p>并行执行向量检索和 BM25 全文检索，通过 RRF（Reciprocal Rank Fusion）融合两路结果。
 * 支持通过 {@link RetrievalProperties#isHybridEnabled()} 一键回退为纯向量检索</p>
 *
 * <p>RRF 公式：score(d) = Σ 1/(k + rank_i(d))，其中 k 默认 60</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HybridSearchService implements RetrievalStrategy {

    private final SemanticSearchService semanticSearchService;
    private final FullTextSearchService fullTextSearchService;
    private final RetrievalProperties retrievalProperties;
    private final ResultFusionStrategy resultFusionStrategy;

    @Override
    public List<SearchResult> retrieve(String queryText, Long knowledgeBaseId, int topK, double vectorThreshold) {
        if (!retrievalProperties.isHybridEnabled()) {
            return semanticSearchService.searchWithPgVector(queryText, knowledgeBaseId, topK, vectorThreshold);
        }

        long start = System.currentTimeMillis();
        int candidateSize = Math.max(topK * 2, 20);

        CompletableFuture<List<SearchResult>> vectorFuture = CompletableFuture.supplyAsync(() ->
                semanticSearchService.searchWithPgVector(queryText, knowledgeBaseId, candidateSize, vectorThreshold));

        CompletableFuture<List<SearchResult>> bm25Future = CompletableFuture.supplyAsync(() ->
                fullTextSearchService.search(queryText, knowledgeBaseId, candidateSize,
                        retrievalProperties.getBm25Threshold()));

        List<SearchResult> vectorResults = vectorFuture.join();
        List<SearchResult> bm25Results = bm25Future.join();
        List<SearchResult> fused = resultFusionStrategy.fuse(
                vectorResults, bm25Results, topK, retrievalProperties.getRrfK());

        log.info("混合检索完成: vector={}, bm25={}, fused={}, elapsedMs={}",
                vectorResults.size(), bm25Results.size(), fused.size(), System.currentTimeMillis() - start);
        return fused;
    }
}
