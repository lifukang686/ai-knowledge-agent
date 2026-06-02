package com.fukang.knowledge.agent.application.rag;

import com.fukang.knowledge.agent.domain.rag.model.SearchResult;
import com.fukang.knowledge.agent.domain.rag.service.RetrievalStrategy;
import com.fukang.knowledge.agent.infrastructure.config.RetrievalProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * RAG 检索服务，负责知识库召回以及改写查询结果不足时的原始问题补召回。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagRetrievalService {

    private final RetrievalStrategy retrievalStrategy;
    private final RetrievalProperties retrievalProperties;

    public List<SearchResult> retrieveWithFallback(String rewrittenQuery, String originalQuery, Long knowledgeBaseId) {
        int topK = retrievalProperties.getTopK();
        double threshold = retrievalProperties.getSimilarityThreshold();

        List<SearchResult> rewrittenResults = retrievalStrategy.retrieve(
                rewrittenQuery, knowledgeBaseId, topK, threshold);

        if (rewrittenResults.size() >= topK || Objects.equals(rewrittenQuery, originalQuery)) {
            return rewrittenResults;
        }

        log.info("Rewritten query has insufficient results({} < {}), supplementing with original query",
                rewrittenResults.size(), topK);
        List<SearchResult> originalResults = retrievalStrategy.retrieve(
                originalQuery, knowledgeBaseId, topK, threshold);

        Set<Long> existingChunkIds = rewrittenResults.stream()
                .map(SearchResult::chunkId)
                .collect(Collectors.toSet());

        List<SearchResult> allResults = new ArrayList<>(rewrittenResults);
        for (SearchResult result : originalResults) {
            if (!existingChunkIds.contains(result.chunkId())) {
                allResults.add(result);
                existingChunkIds.add(result.chunkId());
            }
        }
        allResults.sort(Comparator.comparingDouble(SearchResult::similarity).reversed());
        if (allResults.size() > topK) {
            allResults = allResults.subList(0, topK);
        }
        return allResults;
    }
}
