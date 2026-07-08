package com.fukang.knowledge.agent.module.knowledge.service.chunk;

import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.module.knowledge.model.entity.DocumentChunkEntity;
import com.fukang.knowledge.agent.module.knowledge.model.vo.ChunkResult;
import com.fukang.knowledge.agent.module.knowledge.model.vo.ParsedChunk;
import com.fukang.knowledge.agent.module.knowledge.model.vo.ChunkStorageResult;
import com.fukang.knowledge.agent.module.knowledge.service.storage.DocumentChunkStorageService;
import com.fukang.knowledge.agent.module.modelruntime.service.storage.EmbeddingIndexStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 文档块应用服务。
 * <p>负责校验分块结果，并以替换方式写入 document_chunk，确保重跑管道时旧块和旧向量不会残留。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentChunkService {

    private final DocumentChunkStorageService chunkStorageService;
    private final EmbeddingIndexStorageService embeddingIndexStorageService;

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
                .map(DocumentChunkEntity::getId)
                .filter(Objects::nonNull)
                .toList();
        if (!oldChunkIds.isEmpty()) {
            // chunk 会被重建，旧向量必须同步移除，避免 RAG 检索命中过期内容。
            embeddingIndexStorageService.deleteByChunkIdsPgVector(oldChunkIds);
            log.info("已清除旧文档块向量: documentId={}, chunkCount={}", documentId, oldChunkIds.size());
        }

        int deleted = chunkStorageService.deleteByDocumentId(documentId);
        log.info("已清除旧文档块: documentId={}, deletedCount={}", documentId, deleted);

        List<DocumentChunkEntity> chunkDOs = chunkStorageService.toChunkDOList(chunkResult, documentId);
        ChunkStorageResult result = chunkStorageService.saveBatch(chunkDOs, documentId);

        log.info("文档块替换存储完成: documentId={}, total={}, success={}",
                documentId, result.getTotalCount(), result.getSuccessCount());
        return result;
    }

    /**
     * 校验分块结果，避免空块、空文本或乱序块进入持久化层。
     */
    private void validateChunkResult(ChunkResult chunkResult, Long documentId) {
        if (chunkResult == null) {
            log.warn("分块结果为空，无法存储: documentId={}", documentId);
            throw new BaseException(ErrorCodeEnum.CHUNK_DATA_EMPTY);
        }
        if (documentId == null) {
            log.warn("文档ID为空，无法存储文档块");
            throw new BaseException(ErrorCodeEnum.CHUNK_VALIDATION_FAILED);
        }
        if (chunkResult.getChunks() == null || chunkResult.getChunks().isEmpty()) {
            log.warn("分块列表为空: documentId={}", documentId);
            throw new BaseException(ErrorCodeEnum.CHUNK_DATA_EMPTY);
        }

        List<ParsedChunk> chunks = chunkResult.getChunks();
        List<String> validationErrors = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            ParsedChunk chunk = chunks.get(i);
            if (chunk.getChunkText() == null || chunk.getChunkText().isBlank()) {
                validationErrors.add("块 " + chunk.getChunkOrder() + " 文本内容为空");
            }
            if (chunk.getChunkOrder() != i) {
                validationErrors.add("块顺序号不连续，期望 " + i + "，实际 " + chunk.getChunkOrder());
            }
            if (chunk.getTokenCount() < 0) {
                validationErrors.add("块 " + chunk.getChunkOrder() + " token 数为负数");
            }
        }

        if (!validationErrors.isEmpty()) {
            log.warn("文档块数据校验失败: documentId={}, errors={}", documentId, validationErrors);
            throw new BaseException(ErrorCodeEnum.CHUNK_VALIDATION_FAILED);
        }

        log.info("文档块数据校验通过: documentId={}, chunkCount={}, strategy={}",
                documentId, chunks.size(), chunkResult.getStrategyName());
    }
}
