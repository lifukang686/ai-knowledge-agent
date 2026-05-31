package com.fukang.knowledge.agent.application.knowledge.embedding;

import com.fukang.knowledge.agent.infrastructure.persistence.DocumentChunkStorageService;
import com.fukang.knowledge.agent.infrastructure.ai.EmbeddingIndexStorageService;
import com.fukang.knowledge.agent.domain.knowledge.model.ChunkStorageResult;
import com.fukang.knowledge.agent.domain.knowledge.model.EmbeddingResult;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.DocumentChunkDO;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.KnowledgeBaseDO;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.DocumentMapper;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.KnowledgeBaseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 文档嵌入应用服务
 * <p>知识库管理模块中"向量化与存储"步骤的应用层编排服务。
 * 负责协调文档块文本提取、向量嵌入计算和向量索引持久化的完整流程</p>
 *
 * <p>核心职责：
 * <ul>
 *   <li>从已持久化的文档块中提取文本内容</li>
 *   <li>委托 {@link EmbeddingService} 执行向量嵌入计算</li>
 *   <li>委托 {@link EmbeddingIndexStorageService} 将向量索引持久化到数据库</li>
 *   <li>提供结果反馈，返回向量存储结果信息</li>
 * </ul>
 *
 * <p>在文档入库流程中的位置：
 * <pre>{@code
 * 文档上传 → 解析(DocumentProcessingService) → 分块 → 存储(DocumentChunkAppService)
 *                                                     → 向量化与存储(DocumentEmbeddingAppService)
 * }</pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentEmbeddingAppService {

    private final EmbeddingService embeddingService;
    private final EmbeddingIndexStorageService embeddingIndexStorageService;
    private final DocumentChunkStorageService chunkStorageService;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final DocumentMapper documentMapper;

    /**
     * 对文档块执行向量嵌入并存储（全事务模式）
     * <p>先调用嵌入模型将文本列表转为向量，再将向量索引持久化到数据库。
     * 任意步骤失败时整体回滚。适用于对数据一致性要求高的场景</p>
     *
     * @param documentId      关联的文档ID
     * @param knowledgeBaseId 所属知识库ID
     * @return 向量存储结果，包含成功/失败计数
     * @throws BaseException 文本为空时抛出 CHUNK_DATA_EMPTY，
     *                       嵌入模型不可用时抛出 NO_EMBEDDING_MODEL_AVAILABLE，
     *                       嵌入失败时抛出 EMBEDDING_FAILED,
     *                       存储失败时抛出 VECTOR_STORAGE_FAILED
     */
    @Transactional(rollbackFor = Exception.class)
    public ChunkStorageResult embedAndStore(Long documentId, Long knowledgeBaseId) {
        List<DocumentChunkDO> chunks = chunkStorageService.findByDocumentId(documentId);
        return embedAndStoreWithChunks(chunks, knowledgeBaseId);
    }

    /**
     * 对指定的文档块列表执行向量嵌入并存储（全事务模式）
     * <p>适用于块列表已经确定、无需再查数据库的场景</p>
     *
     * @param chunks          已持久化的文档块列表（需含有效 ID）
     * @param knowledgeBaseId 所属知识库ID
     * @return 向量存储结果
     */
    @Transactional(rollbackFor = Exception.class)
    public ChunkStorageResult embedAndStoreWithChunks(List<DocumentChunkDO> chunks, Long knowledgeBaseId) {
        validateChunks(chunks, knowledgeBaseId);

        List<String> texts = extractTexts(chunks);
        log.info("开始文档块向量化与存储: chunkCount={}, knowledgeBaseId={}", texts.size(), knowledgeBaseId);

        Long embeddingModelId = resolveEmbeddingModelId(knowledgeBaseId);
        EmbeddingResult embeddingResult = embeddingService.embed(texts, embeddingModelId);
        updateEmbeddingMetadata(chunks, embeddingResult);

        return embeddingIndexStorageService.saveVectorsToPgVector(chunks, embeddingResult, knowledgeBaseId);
    }

    /**
     * 对文档块执行向量嵌入并存储（允许部分失败模式）
     * <p>先调用嵌入模型将文本列表转为向量，再逐条持久化向量索引。
     * 单条失败不中断，收集全部失败信息后返回</p>
     *
     * @param chunks          已持久化的文档块列表
     * @param knowledgeBaseId 所属知识库ID
     * @return 向量存储结果，包含失败详情
     */
    public ChunkStorageResult embedAndStoreWithPartialFailure(List<DocumentChunkDO> chunks, Long knowledgeBaseId) {
        validateChunks(chunks, knowledgeBaseId);

        List<String> texts = extractTexts(chunks);
        log.info("开始文档块向量化与存储（允许部分失败）: chunkCount={}, knowledgeBaseId={}",
                texts.size(), knowledgeBaseId);

        Long embeddingModelId = resolveEmbeddingModelId(knowledgeBaseId);
        EmbeddingResult embeddingResult = embeddingService.embed(texts, embeddingModelId);
        updateEmbeddingMetadata(chunks, embeddingResult);

        return embeddingIndexStorageService.saveVectorsToPgVector(chunks, embeddingResult, knowledgeBaseId);
    }

    /**
     * 对文档块执行向量嵌入并批量存储（高效批量模式）
     * <p>使用 MyBatis-Plus saveBatch 在一个事务中批量插入向量索引。
     * 适用于大量文档块的场景，性能优于逐条插入</p>
     *
     * @param chunks          已持久化的文档块列表
     * @param knowledgeBaseId 所属知识库ID
     * @return 向量存储结果
     */
    @Transactional(rollbackFor = Exception.class)
    public ChunkStorageResult embedAndStoreBatch(List<DocumentChunkDO> chunks, Long knowledgeBaseId) {
        validateChunks(chunks, knowledgeBaseId);

        List<String> texts = extractTexts(chunks);
        log.info("开始文档块向量化与批量存储: chunkCount={}, knowledgeBaseId={}",
                texts.size(), knowledgeBaseId);

        Long embeddingModelId = resolveEmbeddingModelId(knowledgeBaseId);
        EmbeddingResult embeddingResult = embeddingService.embed(texts, embeddingModelId);
        updateEmbeddingMetadata(chunks, embeddingResult);

        return embeddingIndexStorageService.saveVectorsToPgVector(chunks, embeddingResult, knowledgeBaseId);
    }

    /**
     * 替换式存储：先清除旧的向量索引，再重新向量化并存储
     * <p>适用于文档重新处理或更新场景，确保不会残留旧数据</p>
     *
     * @param chunks          新的文档块列表
     * @param knowledgeBaseId 所属知识库ID
     * @return 向量存储结果
     */
    @Transactional(rollbackFor = Exception.class)
    public ChunkStorageResult replaceEmbedAndStore(List<DocumentChunkDO> chunks, Long knowledgeBaseId) {
        validateChunks(chunks, knowledgeBaseId);

        embeddingIndexStorageService.deleteByKnowledgeBaseIdPgVector(knowledgeBaseId);
        log.info("已清除旧向量索引: knowledgeBaseId={}", knowledgeBaseId);

        return embedAndStoreBatch(chunks, knowledgeBaseId);
    }

    /**
     * 根据知识库ID查询所有向量索引
     *
     * @deprecated 自 pgvector 完全迁移后，不再能通过 MyBatis-Plus 返回向量索引。向量数据完全由 PgVectorEmbeddingStore 管理
     * @param knowledgeBaseId 知识库ID
     * @return 该知识库下所有向量索引（始终为空列表）
     */
    @Deprecated
    public List<DocumentChunkDO> getVectorsByKnowledgeBase(Long knowledgeBaseId) {
        if (knowledgeBaseId == null) {
            log.warn("知识库ID为空，无法查询向量索引");
            throw new BaseException(ErrorCodeEnum.KNOWLEDGE_BASE_NOT_EXIST);
        }
        log.warn("getVectorsByKnowledgeBase 已废弃，pgvector 中向量与 chunkText 绑定，请使用块查询替代");
        return Collections.emptyList();
    }

    /**
     * 根据知识库ID删除所有向量索引
     *
     * @param knowledgeBaseId 知识库ID
     * @return 删除的记录数量
     */
    public int deleteVectorsByKnowledgeBase(Long knowledgeBaseId) {
        if (knowledgeBaseId == null) {
            log.warn("知识库ID为空，无法删除向量索引");
            throw new BaseException(ErrorCodeEnum.KNOWLEDGE_BASE_NOT_EXIST);
        }
        embeddingIndexStorageService.deleteByKnowledgeBaseIdPgVector(knowledgeBaseId);
        log.info("已通过 pgvector 删除知识库所有向量: knowledgeBaseId={}", knowledgeBaseId);
        return 0;
    }

    /**
     * 校验文档块列表的有效性
     *
     * @param chunks          文档块列表
     * @param knowledgeBaseId 知识库ID
     * @throws BaseException 块列表为空时抛出 CHUNK_DATA_EMPTY
     */
    private void validateChunks(List<DocumentChunkDO> chunks, Long knowledgeBaseId) {
        if (chunks == null || chunks.isEmpty()) {
            log.warn("文档块列表为空，无法执行向量化: knowledgeBaseId={}", knowledgeBaseId);
            throw new BaseException(ErrorCodeEnum.CHUNK_DATA_EMPTY);
        }
        if (knowledgeBaseId == null) {
            log.warn("知识库ID为空，无法存储向量索引");
            throw new BaseException(ErrorCodeEnum.KNOWLEDGE_BASE_NOT_EXIST);
        }
    }

    /**
     * 从文档块列表中提取文本内容
     *
     * @param chunks 文档块列表
     * @return 文本列表，顺序与块顺序一致
     */
    private List<String> extractTexts(List<DocumentChunkDO> chunks) {
        List<String> texts = new ArrayList<>(chunks.size());
        for (DocumentChunkDO chunk : chunks) {
            texts.add(chunk.getChunkText());
        }
        return texts;
    }

    private Long resolveEmbeddingModelId(Long knowledgeBaseId) {
        KnowledgeBaseDO knowledgeBase = knowledgeBaseMapper.selectById(knowledgeBaseId);
        return knowledgeBase != null ? knowledgeBase.getEmbeddingModelId() : null;
    }

    private void updateEmbeddingMetadata(List<DocumentChunkDO> chunks, EmbeddingResult embeddingResult) {
        Long modelId = embeddingResult.modelId();
        Integer dimension = embeddingResult.dimension();
        String version = embeddingResult.modelVersion();

        for (DocumentChunkDO chunk : chunks) {
            chunk.setEmbeddingModelId(modelId);
            chunk.setEmbeddingDimension(dimension);
            chunk.setEmbeddingVersion(version);
            chunkStorageService.updateById(chunk);
        }

        Long documentId = chunks.get(0).getDocumentId();
        com.fukang.knowledge.agent.infrastructure.persistence.entity.DocumentDO document =
                documentMapper.selectById(documentId);
        if (document != null) {
            document.setEmbeddingModelId(modelId);
            document.setEmbeddingDimension(dimension);
            document.setEmbeddingVersion(version);
            documentMapper.updateById(document);
        }
    }
}
