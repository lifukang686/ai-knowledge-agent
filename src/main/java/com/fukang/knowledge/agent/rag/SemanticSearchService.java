package com.fukang.knowledge.agent.rag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fukang.knowledge.agent.application.knowledge.EmbeddingService;
import com.fukang.knowledge.agent.application.knowledge.model.EmbeddingResult;
import com.fukang.knowledge.agent.infrastructure.ai.Langchain4jEmbeddingStoreFactory;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.DocumentChunkDO;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.EmbeddingIndexDO;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.DocumentChunkMapper;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.EmbeddingIndexMapper;
import com.fukang.knowledge.agent.rag.config.RetrievalProperties;
import com.fukang.knowledge.agent.rag.model.SearchResult;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 语义检索核心服务
 * <p>基于 pgvector 原生向量检索实现语义检索功能，是 RAG 模块的核心检索组件。
 * 现已升级为 pgvector + langchain4j PgVectorEmbeddingStore 架构，
 * 相似度计算由数据库层完成，大幅提升大规模检索性能。
 * 原应用层遍历检索方法已标记为 @Deprecated，保留供回退兼容</p>
 *
 * <p>检索流程（pgvector）：
 * <ol>
 *   <li>输入校验：对空查询文本做快速失败处理</li>
 *   <li>查询向量化：将用户查询文本通过嵌入服务转换为向量表示</li>
 *   <li>pgvector 检索：通过 PgVectorEmbeddingStore 在数据库层执行余弦相似度检索</li>
 *   <li>结果组装：构建包含文本、相似度分数和元数据的 SearchResult 列表</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SemanticSearchService {

    private final EmbeddingService embeddingService;
    private final EmbeddingIndexMapper embeddingIndexMapper;
    private final DocumentChunkMapper documentChunkMapper;
    private final RetrievalProperties retrievalProperties;
    private final ObjectMapper objectMapper;
    private final Langchain4jEmbeddingStoreFactory storeFactory;

    /**
     * 执行语义检索，返回与查询文本最相似的文档块结果
     * <p>当知识库中无索引数据或所有向量相似度均低于阈值时返回空列表。
     * 结果按相似度降序排列，最多返回 topK 条</p>
     *
     * @param queryText       用户查询文本，不能为 null 或空白
     * @param knowledgeBaseId 目标知识库 ID
     * @param topK            最大返回结果数
     * @param threshold       相似度阈值 (0.0 ~ 1.0)，低于此值的结果将被过滤
     * @return 语义检索结果列表，按相似度降序排列；输入无效或无匹配时返回空列表
     * @deprecated 请使用 {@link #searchWithPgVector(String, Long, int, double)} 替代，
     *             该方法将在未来版本中移除，当前保留仅用于回退兼容
     */
    @Deprecated
    public List<SearchResult> search(String queryText, Long knowledgeBaseId, int topK, double threshold) {
        long startTime = System.currentTimeMillis();

        if (queryText == null || queryText.isBlank()) {
            log.warn("查询文本为空，跳过语义检索");
            return List.of();
        }

        EmbeddingResult result = embeddingService.embed(List.of(queryText));
        float[] queryVector = result.embeddings().get(0).vector();

        List<EmbeddingIndexDO> indexRecords = embeddingIndexMapper.selectList(
                new LambdaQueryWrapper<EmbeddingIndexDO>()
                        .eq(EmbeddingIndexDO::getKnowledgeBaseId, knowledgeBaseId));

        if (indexRecords == null || indexRecords.isEmpty()) {
            log.warn("知识库 [{}] 无向量索引数据", knowledgeBaseId);
            return List.of();
        }

        List<RecordSimilarity> candidates = new ArrayList<>();

        for (EmbeddingIndexDO record : indexRecords) {
            float[] docVector = SimilarityCalculator.parseVector(record.getVector());
            if (docVector == null) {
                log.debug("向量解析失败，跳过记录: indexId={}, chunkId={}", record.getId(), record.getChunkId());
                continue;
            }

            double similarity = SimilarityCalculator.cosineSimilarity(queryVector, docVector);
            if (similarity < threshold) {
                continue;
            }

            candidates.add(new RecordSimilarity(record, similarity));
        }

        candidates.sort(Comparator.comparingDouble(RecordSimilarity::similarity).reversed());

        if (candidates.isEmpty()) {
            log.info("所有向量相似度均低于阈值 [{}]", threshold);
            return List.of();
        }

        List<RecordSimilarity> topCandidates = candidates.subList(0, Math.min(topK, candidates.size()));

        List<Long> chunkIds = topCandidates.stream()
                .map(c -> c.record().getChunkId())
                .distinct()
                .toList();

        Map<Long, String> chunkTextMap = new HashMap<>();
        if (!chunkIds.isEmpty()) {
            List<DocumentChunkDO> chunks = documentChunkMapper.selectBatchIds(chunkIds);
            if (chunks != null) {
                for (DocumentChunkDO chunk : chunks) {
                    if (chunk != null) {
                        chunkTextMap.put(chunk.getId(), chunk.getChunkText());
                    }
                }
            }
        }

        List<SearchResult> searchResults = new ArrayList<>();
        for (RecordSimilarity candidate : topCandidates) {
            EmbeddingIndexDO record = candidate.record();
            Long chunkId = record.getChunkId();
            String chunkText = chunkTextMap.getOrDefault(chunkId, "");
            searchResults.add(new SearchResult(chunkId, chunkText, candidate.similarity(), record.getMetadata()));
        }

        long elapsedMs = System.currentTimeMillis() - startTime;
        log.info("语义检索完成: query=[{}], knowledgeBaseId=[{}], topK=[{}], threshold=[{}], resultCount=[{}], elapsedMs=[{}]",
                queryText, knowledgeBaseId, topK, threshold, searchResults.size(), elapsedMs);

        return searchResults;
    }

    /**
     * 使用 pgvector 原生检索执行语义检索
     * <p>通过 PgVectorEmbeddingStore 在数据库层完成向量相似度计算，
     * 避免应用层全量遍历，大幅提升检索性能。结果按相似度降序排列</p>
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

        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(topK)
                .minScore(threshold)
                .build();

        EmbeddingSearchResult<TextSegment> searchResult = store.search(searchRequest);

        List<SearchResult> searchResults = new ArrayList<>();
        for (var match : searchResult.matches()) {
            TextSegment segment = match.embedded();
            dev.langchain4j.data.document.Metadata metadata = segment.metadata();

            String chunkIdStr = metadata.getString("chunk_id");
            Long chunkId = chunkIdStr != null ? Long.valueOf(chunkIdStr) : null;
            String chunkText = segment.text();
            String metadataJson;
            try {
                metadataJson = objectMapper.writeValueAsString(metadata.toMap());
            } catch (Exception e) {
                metadataJson = "{}";
            }

            searchResults.add(new SearchResult(chunkId, chunkText, match.score(), metadataJson));
        }

        long elapsedMs = System.currentTimeMillis() - startTime;
        log.info("pgvector 语义检索完成: query=[{}], knowledgeBaseId=[{}], topK=[{}], threshold=[{}], resultCount=[{}], elapsedMs=[{}]",
                queryText, knowledgeBaseId, topK, threshold, searchResults.size(), elapsedMs);

        return searchResults;
    }

    /**
     * 向量索引记录与相似度分数的内部记录类
     *
     * @param record     向量索引记录
     * @param similarity 余弦相似度分数 (0.0 ~ 1.0)
     */
    private record RecordSimilarity(EmbeddingIndexDO record, double similarity) {
    }
}