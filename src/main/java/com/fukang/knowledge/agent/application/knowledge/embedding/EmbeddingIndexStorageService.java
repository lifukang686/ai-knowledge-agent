package com.fukang.knowledge.agent.application.knowledge.embedding;

import com.fukang.knowledge.agent.application.knowledge.chunk.model.ChunkStorageResult;
import com.fukang.knowledge.agent.application.knowledge.embedding.model.EmbeddingResult;
import com.fukang.knowledge.agent.application.knowledge.embedding.model.EmbeddingResult.EmbeddingVector;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.infrastructure.ai.Langchain4jEmbeddingStoreFactory;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.DocumentChunkDO;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 嵌入向量索引存储服务
 * <p>完全基于 pgvector 原生向量存储，通过 langchain4j PgVectorEmbeddingStore
 * 进行向量的写入和删除操作。chunkText 直接存入 pgvector 的 metadata 关联中，
 * 语义检索时直接从 pgvector 返回完整结果，无需跨表查询</p>
 */
@Slf4j
@Service
public class EmbeddingIndexStorageService {

    private final Langchain4jEmbeddingStoreFactory storeFactory;

    public EmbeddingIndexStorageService(Langchain4jEmbeddingStoreFactory storeFactory) {
        this.storeFactory = storeFactory;
    }

    /**
     * 将向量化结果批量写入 pgvector
     * <p>遍历 embeddingResult 中的每个 EmbeddingVector，通过 chunkOrder
     * 找到对应的 DocumentChunkDO，构建 Metadata 后将 Embedding 和 TextSegment
     * 逐条写入 PgVectorEmbeddingStore</p>
     *
     * @param chunks          已持久化的文档块列表（需含有效 ID、chunkText）
     * @param embeddingResult 向量嵌入结果
     * @param knowledgeBaseId 所属知识库 ID
     * @return 全部成功的存储结果
     * @throws BaseException 向量写入失败时抛出 VECTOR_STORAGE_FAILED
     */
    public ChunkStorageResult saveVectorsToPgVector(List<DocumentChunkDO> chunks,
                                                     EmbeddingResult embeddingResult,
                                                     Long knowledgeBaseId) {
        if (chunks == null || chunks.isEmpty()) {
            log.warn("文档块列表为空，无法写入 pgvector");
            throw new BaseException(ErrorCodeEnum.CHUNK_DATA_EMPTY);
        }

        PgVectorEmbeddingStore store = storeFactory.createEmbeddingStore();

        try {
            for (EmbeddingVector embeddingVector : embeddingResult.embeddings()) {
                int chunkOrder = embeddingVector.chunkOrder();
                if (chunkOrder >= chunks.size()) {
                    log.warn("向量顺序号超出块列表范围: chunkOrder={}, chunkListSize={}",
                            chunkOrder, chunks.size());
                    continue;
                }

                DocumentChunkDO chunk = chunks.get(chunkOrder);

                Metadata metadata = new Metadata();
                metadata.put("chunk_id", String.valueOf(chunk.getId()));
                metadata.put("knowledge_base_id", String.valueOf(knowledgeBaseId));
                metadata.put("document_id", String.valueOf(chunk.getDocumentId()));
                metadata.put("chunk_index", String.valueOf(chunk.getChunkOrder()));
                metadata.put("total_chunks", String.valueOf(chunks.size()));

                TextSegment segment = TextSegment.from(chunk.getChunkText(), metadata);
                Embedding embedding = Embedding.from(embeddingVector.vector());

                store.add(embedding, segment);
            }
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("pgvector 批量写入失败: knowledgeBaseId={}, chunkCount={}",
                    knowledgeBaseId, chunks.size(), e);
            throw new BaseException(ErrorCodeEnum.VECTOR_STORAGE_FAILED);
        }

        log.info("pgvector 批量写入完成: knowledgeBaseId={}, count={}",
                knowledgeBaseId, chunks.size());

        return ChunkStorageResult.allSuccess(knowledgeBaseId, chunks.size());
    }

    /**
     * 根据知识库 ID 删除 pgvector 中所有向量记录
     *
     * @param knowledgeBaseId 知识库 ID
     */
    public void deleteByKnowledgeBaseIdPgVector(Long knowledgeBaseId) {
        storeFactory.deleteByKnowledgeBaseId(knowledgeBaseId);
    }

    /**
     * 根据块 ID 列表删除 pgvector 中对应的向量记录
     *
     * @param chunkIds 块 ID 列表
     */
    public void deleteByChunkIdsPgVector(List<Long> chunkIds) {
        storeFactory.deleteByChunkIds(chunkIds);
    }
}