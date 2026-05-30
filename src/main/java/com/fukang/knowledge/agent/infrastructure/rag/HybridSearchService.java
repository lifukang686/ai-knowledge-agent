package com.fukang.knowledge.agent.infrastructure.rag;

import com.fukang.knowledge.agent.domain.rag.model.SearchResult;
import com.fukang.knowledge.agent.infrastructure.config.RetrievalProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
public class HybridSearchService {

    private final SemanticSearchService semanticSearchService;
    private final FullTextSearchService fullTextSearchService;
    private final RetrievalProperties retrievalProperties;

    /**
     * 混合检索：向量 + BM25 → RRF 融合
     * <p>两路独立检索各取 candidateSize 个候选，再通过 RRF 融合取 topK 个结果。
     * 当 hybridEnabled=false 时，退化为纯向量检索</p>
     *
     * @param queryText       查询文本
     * @param knowledgeBaseId 知识库 ID
     * @param topK            最终返回数量
     * @param vectorThreshold 向量检索最小相似度
     * @return 融合后的结果列表
     */
    public List<SearchResult> search(String queryText, Long knowledgeBaseId, int topK, double vectorThreshold) {
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

        List<SearchResult> fused = rrfFusion(vectorResults, bm25Results, topK, retrievalProperties.getRrfK());

        log.info("混合检索完成: vector={}, bm25={}, fused={}, elapsedMs={}",
                vectorResults.size(), bm25Results.size(), fused.size(),
                System.currentTimeMillis() - start);
        return fused;
    }

    /**
     * Reciprocal Rank Fusion
     */
    private List<SearchResult> rrfFusion(List<SearchResult> vectorResults,
                                          List<SearchResult> bm25Results,
                                          int topK, int k) {
        Map<Long, Double> scores = new LinkedHashMap<>();
        Map<Long, String> textMap = new LinkedHashMap<>();

        for (int i = 0; i < vectorResults.size(); i++) {
            SearchResult r = vectorResults.get(i);
            scores.merge(r.chunkId(), 1.0 / (k + i + 1), Double::sum);
            textMap.putIfAbsent(r.chunkId(), r.chunkText());
        }

        for (int i = 0; i < bm25Results.size(); i++) {
            SearchResult r = bm25Results.get(i);
            scores.merge(r.chunkId(), 1.0 / (k + i + 1), Double::sum);
            textMap.putIfAbsent(r.chunkId(), r.chunkText());
        }

        List<SearchResult> result = new ArrayList<>();
        scores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(topK)
                .forEach(e -> result.add(
                        new SearchResult(e.getKey(), textMap.get(e.getKey()), e.getValue(), null)));
        return result;
    }
}