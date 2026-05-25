package com.fukang.knowledge.agent.infrastructure.rag;

import com.fukang.knowledge.agent.application.knowledge.embedding.EmbeddingService;
import com.fukang.knowledge.agent.domain.knowledge.model.EmbeddingResult;
import com.fukang.knowledge.agent.domain.rag.model.SearchResult;
import com.fukang.knowledge.agent.infrastructure.ai.Langchain4jEmbeddingStoreFactory;
import com.fukang.knowledge.agent.infrastructure.config.RetrievalProperties;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 语义检索核心服务
 * <p>基于 pgvector 原生向量检索实现语义检索功能，是 RAG 模块的核心检索组件。
 * 完全使用 pgvector + langchain4j PgVectorEmbeddingStore 架构，
 * 相似度计算由数据库层完成，chunkText 直接从 pgvector metadata 返回，
 * 无需跨表查询</p>
 *
 * <p>检索流程（pgvector）：
 * <ol>
 *   <li>输入校验：对空查询文本做快速失败处理</li>
 *   <li>查询向量化：将用户查询文本通过嵌入服务转换为向量表示</li>
 *   <li>pgvector 检索：通过 PgVectorEmbeddingStore 在数据库层执行余弦相似度检索</li>
 *   <li>结果组装：从 TextSegment 中直接获取 chunkText 构建 SearchResult 列表</li>
 * </ol>
 */
@Slf4j
@Service
public class SemanticSearchService {

    private final EmbeddingService embeddingService;
    private final RetrievalProperties retrievalProperties;
    private final Langchain4jEmbeddingStoreFactory storeFactory;

    public SemanticSearchService(EmbeddingService embeddingService,
                                  RetrievalProperties retrievalProperties,
                                  Langchain4jEmbeddingStoreFactory storeFactory) {
        this.embeddingService = embeddingService;
        this.retrievalProperties = retrievalProperties;
        this.storeFactory = storeFactory;
    }

    /**
     * 使用 pgvector 原生检索执行语义检索
     * <p>通过 PgVectorEmbeddingStore 在数据库层完成向量相似度计算，
     * 避免应用层全量遍历，大幅提升检索性能。结果按相似度降序排列。
     * chunkText 直接从 TextSegment.text() 获取，无需跨表查询</p>
     *
     * @param queryText       用户查询文本，不能为 null 或空白
     * @param knowledgeBaseId 目标知识库 ID
     * @param topK            最大返回结果数
     * @param threshold       相似度阈值 (0.0 ~ 1.0)，低于此值的结果将被过滤
     * @return 语义检索结果列表，按相似度降序排列
     */
    public List<SearchResult> searchWithPgVector(String queryText, Long knowledgeBaseId, int topK, double threshold) {
        long startTime = System.currentTimeMillis();

        if (queryText == null || queryText.isBlank()) {
            log.warn("查询文本为空，跳过语义检索");
            return List.of();
        }

        EmbeddingResult result = embeddingService.embed(List.of(queryText));
        float[] queryVector = result.embeddings().get(0).vector();
        Embedding queryEmbedding = Embedding.from(queryVector);

        PgVectorEmbeddingStore store = storeFactory.createEmbeddingStore();

        EmbeddingSearchRequest.EmbeddingSearchRequestBuilder requestBuilder = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(topK)
                .minScore(threshold);

        if (knowledgeBaseId != null) {
            Filter filter = MetadataFilterBuilder.metadataKey("knowledge_base_id")
                    .isEqualTo(String.valueOf(knowledgeBaseId));
            requestBuilder.filter(filter);
        }

        EmbeddingSearchRequest searchRequest = requestBuilder.build();

        EmbeddingSearchResult<TextSegment> searchResult = store.search(searchRequest);

        List<SearchResult> searchResults = new ArrayList<>();
        for (var match : searchResult.matches()) {
            TextSegment segment = match.embedded();
            Metadata metadata = segment.metadata();

            String chunkIdStr = metadata.getString("chunk_id");
            Long chunkId = chunkIdStr != null ? Long.valueOf(chunkIdStr) : null;
            String chunkText = segment.text();
            String metadataStr = metadata.toMap().toString();

            searchResults.add(new SearchResult(chunkId, chunkText, match.score(), metadataStr));
        }

        long elapsedMs = System.currentTimeMillis() - startTime;
        log.info("pgvector 语义检索完成: query=[{}], knowledgeBaseId=[{}], topK=[{}], threshold=[{}], resultCount=[{}], elapsedMs=[{}]",
                queryText, knowledgeBaseId, topK, threshold, searchResults.size(), elapsedMs);

        return searchResults;
    }
}