package com.fukang.knowledge.agent.application.knowledge.processing;

import com.fukang.knowledge.agent.application.knowledge.chunk.DocumentChunkAppService;
import com.fukang.knowledge.agent.application.knowledge.embedding.DocumentEmbeddingAppService;
import com.fukang.knowledge.agent.application.knowledge.embedding.DocumentEmbeddingTextAppService;
import com.fukang.knowledge.agent.application.knowledge.parsing.DocumentProcessingService;
import com.fukang.knowledge.agent.application.knowledge.port.DocumentRepository;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.domain.knowledge.model.ChunkResult;
import com.fukang.knowledge.agent.domain.knowledge.model.ChunkStorageResult;
import com.fukang.knowledge.agent.domain.knowledge.model.DocumentParseResult;
import com.fukang.knowledge.agent.domain.knowledge.model.DocumentStatus;
import com.fukang.knowledge.agent.infrastructure.config.DocumentProcessingProperties;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.DocumentDO;
import com.fukang.knowledge.agent.infrastructure.storage.MinioStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * 文档处理管道编排器。
 * <p>串联解析、分块、向量化和状态更新四个阶段，属于应用层流程编排。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentProcessingPipeline {

    private final DocumentRepository documentRepository;
    private final MinioStorageService minioStorageService;
    private final DocumentProcessingService processingService;
    private final DocumentChunkAppService chunkAppService;
    private final DocumentEmbeddingTextAppService embeddingTextAppService;
    private final DocumentEmbeddingAppService embeddingAppService;
    private final DocumentProcessingProperties properties;

    /**
     * 执行完整的文档处理管道。
     */
    public void execute(Long documentId, Long knowledgeBaseId,
                        String filePath, String fileName) {
        Instant startTime = Instant.now();
        log.info("文档处理管道启动: documentId={}, fileName={}", documentId, fileName);

        try {
            // 解析为纯文本
            DocumentParseResult parseResult = phaseParse(documentId, filePath, fileName);
            // 文本分块
            ChunkStorageResult chunkStorageResult = phaseChunk(documentId, parseResult);
            // 构造 embedding 专用文本
            phaseBuildEmbeddingText(documentId);
            // 文档块生成 embedding
            phaseEmbed(documentId, knowledgeBaseId);
            // 完成处理
            phaseComplete(documentId, chunkStorageResult.successCount(), startTime);
        } catch (Exception e) {
            phaseFailed(documentId, e);
        }
    }

    /**
     * Phase 1: 从 MinIO 读取原文件并解析为纯文本。
     */
    private DocumentParseResult phaseParse(Long documentId, String filePath, String fileName) {
        updateStatus(documentId, DocumentStatus.PARSING, null);
        log.info("Phase 1/5 文档解析: documentId={}", documentId);

        byte[] fileBytes = minioStorageService.readFileBytes(filePath);
        if (fileBytes == null || fileBytes.length == 0) {
            throw new BaseException(ErrorCodeEnum.FILE_EMPTY);
        }

        DocumentParseResult result = processingService.parseDocument(fileBytes, fileName, fileBytes.length);
        log.info("Phase 1/5 解析完成: documentId={}, charCount={}", documentId, result.characterCount());
        return result;
    }

    /**
     * Phase 2: 对解析文本分块，并替换式写入 document_chunk。
     */
    private ChunkStorageResult phaseChunk(Long documentId, DocumentParseResult parseResult) {
        updateStatus(documentId, DocumentStatus.CHUNKING, null);
        log.info("Phase 2/5 文本分块: documentId={}", documentId);

        // 分块入口：这里只编排流程，真正的文本切分会继续委托到 DocumentProcessingService -> ChunkStrategy。
        ChunkResult chunkResult = processingService.chunkDocument(parseResult);
        // 替换式入库：先清理旧 chunk/旧向量，再把本次切出来的新 chunk 写入 document_chunk。
        ChunkStorageResult storageResult = chunkAppService.replaceAndStoreChunks(chunkResult, documentId);
        log.info("Phase 2/5 分块完成: documentId={}, total={}, success={}",
                documentId, storageResult.totalCount(), storageResult.successCount());
        return storageResult;
    }

    /**
     * Phase 3: 构造 embedding 专用文本，补充标题和片段位置上下文。
     */
    private void phaseBuildEmbeddingText(Long documentId) {
        log.info("Phase 3/5 构造 embedding 文本: documentId={}", documentId);
        int updated = embeddingTextAppService.buildAndStore(documentId);
        log.info("Phase 3/5 embedding 文本构造完成: documentId={}, updated={}", documentId, updated);
    }

    /**
     * Phase 4: 对文档块生成 embedding，并按配置执行指数退避重试。
     */
    private void phaseEmbed(Long documentId, Long knowledgeBaseId) {
        updateStatus(documentId, DocumentStatus.EMBEDDING, null);
        log.info("Phase 4/5 嵌入向量: documentId={}", documentId);

        int maxRetries = properties.getEmbedRetryMax();
        long baseSeconds = properties.getEmbedRetryBaseSeconds();
        int maxAttempts = maxRetries + 1;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                ChunkStorageResult result = embeddingAppService.embedAndStore(documentId, knowledgeBaseId);
                log.info("Phase 4/5 嵌入完成: documentId={}, total={}, success={}",
                        documentId, result.totalCount(), result.successCount());
                return;
            } catch (Exception e) {
                if (!shouldRetryEmbedding(e)) {
                    log.error("Phase 4/5 嵌入遇到不可重试错误: documentId={}, attempt={}/{}",
                            documentId, attempt, maxAttempts, e);
                    throw e;
                }
                if (attempt >= maxAttempts) {
                    log.error("Phase 4/5 嵌入最终失败: documentId={}, attempt={}/{}",
                            documentId, attempt, maxAttempts, e);
                    throw new BaseException(ErrorCodeEnum.EMBEDDING_FAILED);
                }
                sleepBeforeRetry(documentId, attempt, baseSeconds, e);
            }
        }
    }

    /**
     * Phase 5: 标记文档处理完成，记录块数和耗时。
     */
    private void phaseComplete(Long documentId, int chunkCount, Instant startTime) {
        long duration = Duration.between(startTime, Instant.now()).toMillis();
        DocumentDO doc = findDocument(documentId);
        doc.setStatus(DocumentStatus.COMPLETED.getCode());
        doc.setChunkCount(chunkCount);
        doc.setProcessingDurationMs(duration);
        doc.setErrorMessage(null);
        documentRepository.updateById(doc);
        log.info("Phase 5/5 文档处理完成: documentId={}, chunkCount={}, duration={}ms",
                documentId, chunkCount, duration);
    }

    /**
     * 失败统一标记，避免异步线程吞掉错误状态。
     */
    private void phaseFailed(Long documentId, Exception e) {
        log.error("文档处理管道失败: documentId={}", documentId, e);
        try {
            DocumentDO doc = findDocument(documentId);
            doc.setStatus(DocumentStatus.FAILED.getCode());
            doc.setErrorMessage(truncateErrorMessage(e.getMessage()));
            documentRepository.updateById(doc);
        } catch (Exception updateEx) {
            log.error("更新文档失败状态异常: documentId={}", documentId, updateEx);
        }
    }

    private void updateStatus(Long documentId, DocumentStatus status, String errorMessage) {
        DocumentDO doc = findDocument(documentId);
        doc.setStatus(status.getCode());
        if (errorMessage != null) {
            doc.setErrorMessage(errorMessage);
        }
        documentRepository.updateById(doc);
    }

    private DocumentDO findDocument(Long documentId) {
        DocumentDO doc = documentRepository.findById(documentId);
        if (doc == null) {
            throw new BaseException(ErrorCodeEnum.DOCUMENT_NOT_EXIST);
        }
        return doc;
    }

    private boolean shouldRetryEmbedding(Exception e) {
        if (!(e instanceof BaseException baseException)) {
            return true;
        }
        int code = baseException.getCode();
        return code == ErrorCodeEnum.EMBEDDING_FAILED.getCode()
                || code == ErrorCodeEnum.VECTOR_STORAGE_FAILED.getCode();
    }

    private void sleepBeforeRetry(Long documentId, int attempt, long baseSeconds, Exception e) {
        long delaySeconds = baseSeconds * (1L << (attempt - 1));
        log.warn("Phase 4/5 嵌入失败（第 {} 次尝试），{} 秒后重试: documentId={}",
                attempt, delaySeconds, documentId, e);
        try {
            Thread.sleep(delaySeconds * 1000);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new BaseException(ErrorCodeEnum.EMBEDDING_FAILED);
        }
    }

    private String truncateErrorMessage(String errorMsg) {
        if (errorMsg != null && errorMsg.length() > 2000) {
            return errorMsg.substring(0, 1997) + "...";
        }
        return errorMsg;
    }
}
