package com.fukang.knowledge.agent.application.knowledge.chunk;

import com.fukang.knowledge.agent.domain.knowledge.model.ChunkResult;
import com.fukang.knowledge.agent.domain.knowledge.model.ChunkResult.DocumentChunk;
import com.fukang.knowledge.agent.domain.knowledge.model.ChunkStorageResult;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.infrastructure.ai.EmbeddingIndexStorageService;
import com.fukang.knowledge.agent.infrastructure.persistence.DocumentChunkStorageService;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.DocumentChunkDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 文档块应用服务
 * <p>知识库管理模块中"分块存储"步骤的应用层编排服务。
 * 负责协调分块结果验证、数据转换和批量持久化操作。
 * 上承 {@link DocumentProcessingService} 的分块结果，
 * 下接 {@link DocumentChunkStorageService} 的存储能力，
 * 是文档入库流程中"解析→分块→存储"管道的终点</p>
 *
 * <p>核心职责：
 * <ul>
 *   <li>接收分块结果并进行数据校验（块文本非空、顺序号连续等）</li>
 *   <li>将模型数据转换为持久化实体，补全关联信息</li>
 *   <li>委托存储服务执行批量写入，返回结构化的存储结果</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentChunkAppService {

    private final DocumentChunkStorageService chunkStorageService;
    private final EmbeddingIndexStorageService embeddingIndexStorageService;

    /**
     * 存储文档分块结果（全事务模式）
     * <p>对分块结果进行数据校验后，在一个事务中完成所有块的持久化。
     * 任意一条失败时整体回滚。适用于对数据一致性要求高的场景</p>
     *
     * @param chunkResult 文档分块结果（来自 DocumentProcessingService）
     * @param documentId  关联的文档ID（对应 document 表主键）
     * @return 存储结果，包含成功/失败计数和失败详情
     * @throws BaseException 数据校验失败或存储异常时抛出对应异常
     */
    @Transactional(rollbackFor = Exception.class)
    public ChunkStorageResult storeChunks(ChunkResult chunkResult, Long documentId) {
        validateChunkResult(chunkResult, documentId);

        List<DocumentChunkDO> chunkDOs = chunkStorageService.toChunkDOList(chunkResult, documentId);
        log.info("文档块数据校验通过，准备存储: documentId={}, chunkCount={}",
                documentId, chunkDOs.size());

        return chunkStorageService.saveAllInTransaction(chunkDOs, documentId);
    }

    /**
     * 存储文档分块结果（允许部分失败模式）
     * <p>对分块结果进行数据校验后，逐条持久化每个块。
     * 单条失败不中断后续存储，返回详细的失败信息。
     * 适用于需要部分成功反馈或大数据量场景</p>
     *
     * @param chunkResult 文档分块结果
     * @param documentId  关联的文档ID
     * @return 存储结果，包含成功/失败计数和每条失败详情
     */
    public ChunkStorageResult storeChunksWithPartialFailure(ChunkResult chunkResult, Long documentId) {
        validateChunkResult(chunkResult, documentId);

        List<DocumentChunkDO> chunkDOs = chunkStorageService.toChunkDOList(chunkResult, documentId);
        log.info("文档块数据校验通过，准备逐条存储: documentId={}, chunkCount={}",
                documentId, chunkDOs.size());

        return chunkStorageService.saveAllWithPartialFailure(chunkDOs, documentId);
    }

    /**
     * 批量存储文档分块结果（高效批量模式）
     * <p>使用 MyBatis-Plus 的 saveBatch 在一个事务中批量插入所有块。
     * 较逐条插入效率更高，适用于大量文档块的场景</p>
     *
     * @param chunkResult 文档分块结果
     * @param documentId  关联的文档ID
     * @return 存储结果
     */
    @Transactional(rollbackFor = Exception.class)
    public ChunkStorageResult storeChunksBatch(ChunkResult chunkResult, Long documentId) {
        validateChunkResult(chunkResult, documentId);

        List<DocumentChunkDO> chunkDOs = chunkStorageService.toChunkDOList(chunkResult, documentId);
        log.info("文档块数据校验通过，准备批量插入: documentId={}, chunkCount={}",
                documentId, chunkDOs.size());

        return chunkStorageService.saveBatch(chunkDOs, documentId);
    }

    /**
     * 对分块结果进行数据校验
     * <p>校验项包括：
     * <ol>
     *   <li>分块结果不为空</li>
     *   <li>文档ID有效</li>
     *   <li>块列表非空</li>
     *   <li>每个块的文本内容非空</li>
     *   <li>块顺序号连续（0, 1, 2, ...）</li>
     * </ol>
     *
     * @param chunkResult 文档分块结果
     * @param documentId  关联的文档ID
     * @throws BaseException 校验失败时抛出 CHUNK_VALIDATION_FAILED
     */
    public void validateChunkResult(ChunkResult chunkResult, Long documentId) {
        if (chunkResult == null) {
            log.warn("分块结果为空，无法存储: documentId={}", documentId);
            throw new BaseException(ErrorCodeEnum.CHUNK_DATA_EMPTY);
        }

        if (documentId == null) {
            log.warn("文档ID为空，无法存储文档块");
            throw new BaseException(ErrorCodeEnum.CHUNK_VALIDATION_FAILED);
        }

        if (chunkResult.chunks() == null || chunkResult.chunks().isEmpty()) {
            log.warn("分块列表为空: documentId={}", documentId);
            throw new BaseException(ErrorCodeEnum.CHUNK_DATA_EMPTY);
        }

        List<DocumentChunk> chunks = chunkResult.chunks();
        List<String> validationErrors = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = chunks.get(i);
            if (chunk.chunkText() == null || chunk.chunkText().isBlank()) {
                validationErrors.add("块 " + chunk.chunkOrder() + " 文本内容为空");
            }
            if (chunk.chunkOrder() != i) {
                validationErrors.add("块顺序号不连续，期望 " + i + "，实际 " + chunk.chunkOrder());
            }
            if (chunk.tokenCount() < 0) {
                validationErrors.add("块 " + chunk.chunkOrder() + " token 数为负数");
            }
        }

        if (!validationErrors.isEmpty()) {
            log.warn("文档块数据校验失败: documentId={}, errors={}", documentId, validationErrors);
            throw new BaseException(ErrorCodeEnum.CHUNK_VALIDATION_FAILED);
        }

        log.info("文档块数据校验通过: documentId={}, chunkCount={}, strategy={}",
                documentId, chunks.size(), chunkResult.strategyName());
    }

    /**
     * 获取文档的所有已存储块
     *
     * @param documentId 文档ID
     * @return 已存储的文档块列表
     */
    public List<DocumentChunkDO> getChunksByDocument(Long documentId) {
        if (documentId == null) {
            log.warn("文档ID为空，无法查询文档块");
            throw new BaseException(ErrorCodeEnum.DOCUMENT_NOT_EXIST);
        }
        return chunkStorageService.findByDocumentId(documentId);
    }

    /**
     * 删除文档的所有块
     *
     * @param documentId 文档ID
     * @return 删除的块数量
     */
    public int deleteChunksByDocument(Long documentId) {
        if (documentId == null) {
            log.warn("文档ID为空，无法删除文档块");
            throw new BaseException(ErrorCodeEnum.DOCUMENT_NOT_EXIST);
        }
        return chunkStorageService.deleteByDocumentId(documentId);
    }

    /**
     * 统计文档的块总数
     *
     * @param documentId 文档ID
     * @return 块数量
     */
    public long countChunksByDocument(Long documentId) {
        if (documentId == null) {
            log.warn("文档ID为空，无法统计文档块");
            throw new BaseException(ErrorCodeEnum.DOCUMENT_NOT_EXIST);
        }
        return chunkStorageService.countByDocumentId(documentId);
    }

    /**
     * 替换式存储文档分块结果。
     * <p>先清理旧向量和旧块，再批量插入新的分块，避免重复入库时残留历史检索数据。</p>
     *
     * @param chunkResult 文档分块结果
     * @param documentId  关联的文档ID
     * @return 存储结果
     */
    @Transactional(rollbackFor = Exception.class)
    public ChunkStorageResult replaceAndStoreChunks(ChunkResult chunkResult, Long documentId) {
        validateChunkResult(chunkResult, documentId);

        List<Long> oldChunkIds = chunkStorageService.findByDocumentId(documentId).stream()
                .map(DocumentChunkDO::getId)
                .filter(Objects::nonNull)
                .toList();
        if (!oldChunkIds.isEmpty()) {
            // chunk 会被重建，旧向量必须同步移除，避免 RAG 检索命中过期内容。
            embeddingIndexStorageService.deleteByChunkIdsPgVector(oldChunkIds);
            log.info("已清除旧文档块向量: documentId={}, chunkCount={}", documentId, oldChunkIds.size());
        }

        int deleted = chunkStorageService.deleteByDocumentId(documentId);
        log.info("已清除旧文档块: documentId={}, deletedCount={}", documentId, deleted);

        // 到这里文本已经切分完成；本方法只负责把 ChunkResult 转成数据库实体并持久化。
        List<DocumentChunkDO> chunkDOs = chunkStorageService.toChunkDOList(chunkResult, documentId);
        ChunkStorageResult result = chunkStorageService.saveBatch(chunkDOs, documentId);

        log.info("文档块替换存储完成: documentId={}, total={}, success={}",
                documentId, result.totalCount(), result.successCount());

        return result;
    }
}
