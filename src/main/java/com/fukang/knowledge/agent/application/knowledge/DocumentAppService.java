package com.fukang.knowledge.agent.application.knowledge;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fukang.knowledge.agent.application.knowledge.port.DocumentChunkRepository;
import com.fukang.knowledge.agent.application.knowledge.port.DocumentRepository;
import com.fukang.knowledge.agent.application.knowledge.result.DocumentDetailResult;
import com.fukang.knowledge.agent.application.knowledge.result.DocumentResult;
import com.fukang.knowledge.agent.application.knowledge.result.DocumentStatusResult;
import com.fukang.knowledge.agent.application.knowledge.result.DocumentUploadResult;
import com.fukang.knowledge.agent.common.context.UserContextHolder;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.common.result.PageResponse;
import com.fukang.knowledge.agent.domain.knowledge.event.DocumentUploadedEvent;
import com.fukang.knowledge.agent.domain.knowledge.model.DocumentStatus;
import com.fukang.knowledge.agent.infrastructure.ai.EmbeddingIndexStorageService;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.DocumentChunkDO;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.DocumentDO;
import com.fukang.knowledge.agent.infrastructure.storage.MinioStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 文档应用服务。
 * <p>负责文档上传、列表、详情、状态查询和删除，避免知识库服务继续膨胀。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentAppService {

    /** 支持上传的文件扩展名列表。 */
    private static final List<String> ALLOWED_EXTENSIONS = List.of(
            "pdf", "doc", "docx", "txt", "md", "xlsx", "xls", "ppt", "pptx"
    );

    private final DocumentRepository documentRepository;
    private final MinioStorageService minioStorageService;
    private final ApplicationEventPublisher eventPublisher;
    private final DocumentChunkRepository documentChunkRepository;
    private final EmbeddingIndexStorageService embeddingIndexStorageService;

    /**
     * 上传文档并发布异步处理事件。
     * <p>数据库只记录原文件元数据，解析、分块和向量化由后续 pipeline 处理。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public DocumentUploadResult uploadDocument(Long knowledgeBaseId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BaseException(ErrorCodeEnum.FILE_EMPTY);
        }

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new BaseException(ErrorCodeEnum.FILE_NAME_EMPTY);
        }
        validateFileExtension(originalFileName);

        String filePath = minioStorageService.uploadFile(file);

        DocumentDO document = new DocumentDO();
        document.setKnowledgeBaseId(knowledgeBaseId);
        document.setTitle(originalFileName);
        document.setFilePath(filePath);
        document.setUploaderId(UserContextHolder.getUserId());
        document.setStatus(DocumentStatus.PENDING.getCode());
        documentRepository.insert(document);

        log.info("文档上传成功: id={}, title={}, knowledgeBaseId={}, path={}, status={}",
                document.getId(), originalFileName, knowledgeBaseId, filePath,
                DocumentStatus.PENDING.getCode());

        eventPublisher.publishEvent(new DocumentUploadedEvent(
                this, document.getId(), knowledgeBaseId, filePath, originalFileName));

        return new DocumentUploadResult(document.getId(), DocumentStatus.PENDING.getCode());
    }

    /**
     * 分页查询文档列表。
     */
    public PageResponse<DocumentResult> listDocuments(Long knowledgeBaseId, long page, long pageSize) {
        IPage<DocumentDO> resultPage = documentRepository.pageByKnowledgeBase(knowledgeBaseId, page, pageSize);

        List<DocumentResult> items = resultPage.getRecords().stream()
                .map(this::toDocumentResult)
                .collect(Collectors.toList());

        return new PageResponse<>(items, resultPage.getTotal(), resultPage.getCurrent(), resultPage.getSize());
    }

    /**
     * 查询文档详情。
     * <p>详情内容读取已解析后的文档块，避免直接加载 PDF、Office 等原始大文件。</p>
     */
    public DocumentDetailResult getDocumentDetail(Long documentId) {
        DocumentDO document = findDocumentById(documentId);

        String content = buildParsedContent(documentId);

        String status = resolveStatus(document);
        String uploadedBy = document.getUploaderId() != null ? document.getUploaderId().toString() : "";

        return new DocumentDetailResult(
                document.getId(),
                document.getTitle(),
                content,
                document.getFilePath(),
                document.getKnowledgeBaseId(),
                status,
                uploadedBy,
                document.getChunkCount() != null ? document.getChunkCount().longValue() : 0L,
                document.getProcessingDurationMs() != null ? document.getProcessingDurationMs() : 0L,
                document.getEmbeddingModelId(),
                document.getEmbeddingDimension(),
                document.getEmbeddingVersion(),
                document.getCreateTime(),
                document.getUpdateTime()
        );
    }

    private String buildParsedContent(Long documentId) {
        return documentChunkRepository.findByDocumentId(documentId).stream()
                .map(DocumentChunkDO::getChunkText)
                .filter(text -> text != null && !text.isBlank())
                .collect(Collectors.joining("\n\n"));
    }

    /**
     * 查询文档处理状态。
     */
    public DocumentStatusResult getDocumentStatus(Long documentId) {
        DocumentDO document = findDocumentById(documentId);
        return new DocumentStatusResult(resolveStatus(document));
    }

    /**
     * 删除单个文档及其 chunk、向量和 MinIO 原文件。
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteDocument(Long documentId) {
        DocumentDO document = findDocumentById(documentId);

        List<Long> chunkIds = documentChunkRepository.findIdsByDocumentId(documentId);

        if (!chunkIds.isEmpty()) {
            embeddingIndexStorageService.deleteByChunkIdsPgVector(chunkIds);
            log.info("文档关联向量索引已删除: documentId={}, chunkCount={}", documentId, chunkIds.size());
        }

        long chunkCount = documentChunkRepository.deleteByDocumentId(documentId);
        log.info("文档关联块已删除: documentId={}, chunkCount={}", documentId, chunkCount);

        documentRepository.deleteById(documentId);
        log.info("文档记录已删除: id={}, title={}, knowledgeBaseId={}",
                documentId, document.getTitle(), document.getKnowledgeBaseId());

        minioStorageService.deleteFile(document.getFilePath());
    }

    private DocumentDO findDocumentById(Long documentId) {
        DocumentDO document = documentRepository.findById(documentId);
        if (document == null) {
            log.warn("文档不存在: id={}", documentId);
            throw new BaseException(ErrorCodeEnum.DOCUMENT_NOT_EXIST);
        }
        return document;
    }

    private String resolveStatus(DocumentDO document) {
        if (document.getStatus() != null && !document.getStatus().isBlank()) {
            return document.getStatus();
        }
        return document.getFilePath() != null ? "uploaded" : "pending";
    }

    private DocumentResult toDocumentResult(DocumentDO doc) {
        String status = resolveStatus(doc);
        String uploadedBy = doc.getUploaderId() != null ? doc.getUploaderId().toString() : "";
        return new DocumentResult(
                doc.getId(),
                doc.getTitle(),
                doc.getFilePath(),
                doc.getKnowledgeBaseId(),
                status,
                uploadedBy,
                doc.getChunkCount() != null ? doc.getChunkCount().longValue() : 0L,
                doc.getProcessingDurationMs() != null ? doc.getProcessingDurationMs() : 0L,
                doc.getCreateTime(),
                doc.getUpdateTime()
        );
    }

    private void validateFileExtension(String fileName) {
        String extension = extractExtension(fileName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            log.warn("不支持的文件类型: fileName={}, extension={}", fileName, extension);
            throw new BaseException(ErrorCodeEnum.FILE_TYPE_NOT_SUPPORTED);
        }
    }

    private String extractExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1).toLowerCase();
    }
}
