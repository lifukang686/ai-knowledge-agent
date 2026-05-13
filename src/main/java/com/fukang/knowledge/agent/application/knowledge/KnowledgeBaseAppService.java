package com.fukang.knowledge.agent.application.knowledge;

import com.fukang.knowledge.agent.api.document.dto.DocumentStatusResp;
import com.fukang.knowledge.agent.api.document.dto.DocumentUploadResp;
import com.fukang.knowledge.agent.common.context.UserContextHolder;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.DocumentDO;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.DocumentMapper;
import com.fukang.knowledge.agent.infrastructure.storage.MinioStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 知识库管理应用服务
 * <p>负责文档入库流程的核心业务编排，包括文档上传、格式验证、
 * 文件存储和数据库记录创建</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseAppService {

    /** 支持上传的文件扩展名列表 */
    private static final List<String> ALLOWED_EXTENSIONS = List.of(
            "pdf", "doc", "docx", "txt", "md", "xlsx", "xls", "ppt", "pptx"
    );

    private final DocumentMapper documentMapper;
    private final MinioStorageService minioStorageService;

    /**
     * 上传文档
     * <p>完整的文档上传业务流程：
     * <ol>
     *   <li>校验上传文件是否为空</li>
     *   <li>校验文件类型是否为支持的格式</li>
     *   <li>将文件存储到 MinIO 对象存储</li>
     *   <li>在 document 表中创建记录，保存文件路径和元数据</li>
     * </ol>
     * 完成后返回文档ID和状态，后续由异步任务负责文档解析和向量化
     *
     * @param knowledgeBaseId 目标知识库ID
     * @param file            上传的文件
     * @return 文档上传响应，包含文档ID和状态
     * @throws BaseException 文件为空、格式不支持或上传失败时抛出对应异常
     */
    @Transactional(rollbackFor = Exception.class)
    public DocumentUploadResp uploadDocument(Long knowledgeBaseId, MultipartFile file) {
        // 1. 校验文件非空
        if (file == null || file.isEmpty()) {
            throw new BaseException(ErrorCodeEnum.FILE_EMPTY);
        }

        // 2. 校验文件名和类型
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new BaseException(ErrorCodeEnum.FILE_NAME_EMPTY);
        }
        validateFileExtension(originalFileName);

        // 3. 上传文件到 MinIO
        String filePath = minioStorageService.uploadFile(file);

        // 4. 创建文档记录
        DocumentDO document = new DocumentDO();
        document.setKnowledgeBaseId(knowledgeBaseId);
        document.setTitle(originalFileName);
        document.setFilePath(filePath);
        document.setUploaderId(UserContextHolder.getUserId());
        documentMapper.insert(document);

        log.info("文档上传成功: id={}, title={}, knowledgeBaseId={}, path={}",
                document.getId(), originalFileName, knowledgeBaseId, filePath);

        return new DocumentUploadResp(document.getId(), "uploaded");
    }

    /**
     * 查询文档处理状态
     * <p>根据文档ID查询文档的当前处理阶段状态，用于前端轮询文档入库进度</p>
     *
     * @param documentId 文档ID
     * @return 文档状态响应，包含状态标识
     * @throws BaseException 文档不存在时抛出 DOCUMENT_NOT_EXIST
     */
    public DocumentStatusResp getDocumentStatus(Long documentId) {
        DocumentDO document = findDocumentById(documentId);
        String status = document.getFilePath() != null ? "uploaded" : "pending";
        return new DocumentStatusResp(status);
    }

    /**
     * 删除文档
     * <p>删除文档时同步清理 MinIO 中存储的原始文件，确保不产生孤立文件。
     * MinIO 文件删除失败不影响数据库记录删除，仅在日志中记录异常。</p>
     *
     * @param documentId 文档ID
     * @throws BaseException 文档不存在时抛出 DOCUMENT_NOT_EXIST
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteDocument(Long documentId) {
        DocumentDO document = findDocumentById(documentId);

        documentMapper.deleteById(documentId);
        log.info("文档已删除: id={}, title={}, knowledgeBaseId={}",
                documentId, document.getTitle(), document.getKnowledgeBaseId());

        // MinIO 文件删除放在数据库删除之后，避免文件删除失败阻塞业务
        minioStorageService.deleteFile(document.getFilePath());
    }

    /**
     * 根据ID查询文档，不存在时抛出异常
     *
     * @param documentId 文档ID
     * @return 文档实体
     * @throws BaseException 文档不存在时抛出 DOCUMENT_NOT_EXIST
     */
    private DocumentDO findDocumentById(Long documentId) {
        DocumentDO document = documentMapper.selectById(documentId);
        if (document == null) {
            log.warn("文档不存在: id={}", documentId);
            throw new BaseException(ErrorCodeEnum.DOCUMENT_NOT_EXIST);
        }
        return document;
    }

    /**
     * 校验文件扩展名是否在允许列表中
     *
     * @param fileName 原始文件名
     * @throws BaseException 文件类型不支持时抛出 FILE_TYPE_NOT_SUPPORTED
     */
    private void validateFileExtension(String fileName) {
        String extension = extractExtension(fileName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            log.warn("不支持的文件类型: fileName={}, extension={}", fileName, extension);
            throw new BaseException(ErrorCodeEnum.FILE_TYPE_NOT_SUPPORTED);
        }
    }

    /**
     * 提取文件扩展名并转为小写
     *
     * @param fileName 文件名
     * @return 小写的文件扩展名，无扩展名时返回空字符串
     */
    private String extractExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1).toLowerCase();
    }
}