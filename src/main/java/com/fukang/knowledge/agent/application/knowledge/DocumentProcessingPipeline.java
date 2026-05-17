package com.fukang.knowledge.agent.application.knowledge;

import com.fukang.knowledge.agent.application.knowledge.model.ChunkResult;
import com.fukang.knowledge.agent.application.knowledge.model.ChunkStorageResult;
import com.fukang.knowledge.agent.application.knowledge.model.DocumentParseResult;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.infrastructure.config.DocumentProcessingProperties;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.DocumentDO;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.DocumentMapper;
import com.fukang.knowledge.agent.infrastructure.storage.MinioStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * 文档处理管道编排器
 * <p>负责串联文档上传后四个异步处理阶段：
 * <ol>
 *   <li>文档解析 — 从 MinIO 读取文件 → 提取纯文本</li>
 *   <li>文本分块 — 根据策略拆分文本 → 持久化块</li>
 *   <li>嵌入向量 — 调用嵌入模型生成向量 → 持久化向量索引</li>
 *   <li>完成标记 — 更新文档状态为 COMPLETED，记录耗时和块数</li>
 * </ol>
 *
 * <p>每阶段开始前更新文档状态，失败时记录错误信息并标记 FAILED。
 * 嵌入阶段支持指数退避重试，其他阶段失败不重试（直接标记失败）</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentProcessingPipeline {

    private final DocumentMapper documentMapper;
    private final MinioStorageService minioStorageService;
    private final DocumentProcessingService processingService;
    private final DocumentChunkAppService chunkAppService;
    private final DocumentEmbeddingAppService embeddingAppService;
    private final DocumentProcessingProperties properties;

    /**
     * 执行完整的文档处理管道
     *
     * @param documentId      文档ID
     * @param knowledgeBaseId 知识库ID
     * @param filePath        MinIO 文件存储路径
     * @param fileName        原始文件名
     */
    public void execute(Long documentId, Long knowledgeBaseId,
                        String filePath, String fileName) {
        Instant startTime = Instant.now();
        log.info("文档处理管道启动: documentId={}, fileName={}", documentId, fileName);

        try {
            DocumentParseResult parseResult = phaseParse(documentId, filePath, fileName);
            List<Long> chunkIds = phaseChunk(documentId, parseResult);
            phaseEmbed(documentId, knowledgeBaseId);
            phaseComplete(documentId, chunkIds.size(), startTime);
        } catch (Exception e) {
            phaseFailed(documentId, e);
        }
    }

    /**
     * Phase 1: 文档解析
     * <p>从 MinIO 读取文件字节数据 → 调用解析器提取纯文本</p>
     */
    private DocumentParseResult phaseParse(Long documentId, String filePath, String fileName) {
        updateStatus(documentId, DocumentStatus.PARSING, null);
        log.info("Phase 1/4 文档解析: documentId={}", documentId);

        byte[] fileBytes = minioStorageService.readFileBytes(filePath);
        if (fileBytes == null || fileBytes.length == 0) {
            throw new BaseException(ErrorCodeEnum.FILE_EMPTY);
        }

        DocumentParseResult result = processingService.parseDocument(
                fileBytes, fileName, fileBytes.length);
        log.info("Phase 1/4 解析完成: documentId={}, charCount={}",
                documentId, result.characterCount());
        return result;
    }

    /**
     * Phase 2: 文本分块与存储
     * <p>对解析结果分块 → 替换式存储到 document_chunk 表</p>
     */
    private List<Long> phaseChunk(Long documentId, DocumentParseResult parseResult) {
        updateStatus(documentId, DocumentStatus.CHUNKING, null);
        log.info("Phase 2/4 文本分块: documentId={}", documentId);

        ChunkResult chunkResult = processingService.chunkDocument(parseResult);
        List<Long> chunkIds = chunkAppService.replaceAndStoreChunks(chunkResult, documentId);
        log.info("Phase 2/4 分块完成: documentId={}, chunkCount={}",
                documentId, chunkResult.totalChunks());
        return chunkIds;
    }

    /**
     * Phase 3: 嵌入向量与入库（带重试）
     * <p>对文档块执行向量嵌入，失败时指数退避重试，最多重试 N 次</p>
     */
    private void phaseEmbed(Long documentId, Long knowledgeBaseId) {
        updateStatus(documentId, DocumentStatus.EMBEDDING, null);
        log.info("Phase 3/4 嵌入向量: documentId={}", documentId);

        int maxRetries = properties.getEmbedRetryMax();
        long baseSeconds = properties.getEmbedRetryBaseSeconds();

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                ChunkStorageResult result = embeddingAppService.embedAndStore(
                        documentId, knowledgeBaseId);
                log.info("Phase 3/4 嵌入完成: documentId={}, total={}, success={}",
                        documentId, result.totalCount(), result.successCount());
                return;
            } catch (Exception e) {
                if (attempt < maxRetries) {
                    long delaySeconds = baseSeconds * (1L << attempt);
                    log.warn("Phase 3/4 嵌入失败（第 {} 次重试），{} 秒后重试: documentId={}",
                            attempt + 1, delaySeconds, documentId, e);
                    try {
                        Thread.sleep(delaySeconds * 1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new BaseException(ErrorCodeEnum.EMBEDDING_FAILED);
                    }
                } else {
                    throw new BaseException(ErrorCodeEnum.EMBEDDING_FAILED);
                }
            }
        }
    }

    /**
     * Phase 4: 完成标记
     * <p>标记文档为 COMPLETED，记录块数和处理耗时</p>
     */
    private void phaseComplete(Long documentId, int chunkCount, Instant startTime) {
        long duration = Duration.between(startTime, Instant.now()).toMillis();
        DocumentDO doc = findDocument(documentId);
        doc.setStatus(DocumentStatus.COMPLETED.getCode());
        doc.setChunkCount(chunkCount);
        doc.setProcessingDurationMs(duration);
        doc.setErrorMessage(null);
        documentMapper.updateById(doc);
        log.info("Phase 4/4 文档处理完成: documentId={}, chunkCount={}, duration={}ms",
                documentId, chunkCount, duration);
    }

    /**
     * 处理失败统一标记
     * <p>截断过长的错误信息以防止数据库字段溢出</p>
     */
    private void phaseFailed(Long documentId, Exception e) {
        log.error("文档处理管道失败: documentId={}", documentId, e);
        try {
            DocumentDO doc = findDocument(documentId);
            doc.setStatus(DocumentStatus.FAILED.getCode());
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.length() > 2000) {
                errorMsg = errorMsg.substring(0, 1997) + "...";
            }
            doc.setErrorMessage(errorMsg);
            documentMapper.updateById(doc);
        } catch (Exception updateEx) {
            log.error("更新文档失败状态异常: documentId={}", documentId, updateEx);
        }
    }

    /**
     * 更新文档状态（仅状态字段）
     */
    private void updateStatus(Long documentId, DocumentStatus status, String errorMessage) {
        DocumentDO doc = findDocument(documentId);
        doc.setStatus(status.getCode());
        if (errorMessage != null) {
            doc.setErrorMessage(errorMessage);
        }
        documentMapper.updateById(doc);
    }

    /**
     * 查询文档，不存在时抛出异常
     */
    private DocumentDO findDocument(Long documentId) {
        DocumentDO doc = documentMapper.selectById(documentId);
        if (doc == null) {
            throw new BaseException(ErrorCodeEnum.DOCUMENT_NOT_EXIST);
        }
        return doc;
    }
}