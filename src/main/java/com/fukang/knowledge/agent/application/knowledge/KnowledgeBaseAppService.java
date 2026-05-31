package com.fukang.knowledge.agent.application.knowledge;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fukang.knowledge.agent.api.document.dto.DocumentDetailResp;
import com.fukang.knowledge.agent.api.document.dto.DocumentResp;
import com.fukang.knowledge.agent.api.document.dto.DocumentStatusResp;
import com.fukang.knowledge.agent.api.document.dto.DocumentUploadResp;
import com.fukang.knowledge.agent.api.knowledgebase.dto.CreateKnowledgeBaseReq;
import com.fukang.knowledge.agent.api.knowledgebase.dto.KnowledgeBaseResp;
import com.fukang.knowledge.agent.api.knowledgebase.dto.UpdateKnowledgeBaseReq;
import com.fukang.knowledge.agent.domain.knowledge.model.DocumentStatus;
import com.fukang.knowledge.agent.common.context.UserContextHolder;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.common.result.PageResponse;
import com.fukang.knowledge.agent.domain.knowledge.event.DocumentUploadedEvent;
import com.fukang.knowledge.agent.infrastructure.ai.EmbeddingIndexStorageService;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.DocumentChunkDO;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.DocumentDO;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.KnowledgeBaseDO;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.DocumentChunkMapper;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.DocumentMapper;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.KnowledgeBaseMapper;
import com.fukang.knowledge.agent.infrastructure.storage.MinioStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final MinioStorageService minioStorageService;
    private final ApplicationEventPublisher eventPublisher;
    private final DocumentChunkMapper documentChunkMapper;
    private final EmbeddingIndexStorageService embeddingIndexStorageService;

    /**
     * 上传文档
     * <p>完整的文档上传业务流程：
     * <ol>
     *   <li>校验上传文件是否为空</li>
     *   <li>校验文件类型是否为支持的格式</li>
     *   <li>将文件存储到 MinIO 对象存储</li>
     *   <li>在 document 表中创建记录，保存文件路径和元数据</li>
     *   <li>设置文档状态为 PENDING</li>
     *   <li>发布 DocumentUploadedEvent，事务提交后由异步监听器触发处理管道</li>
     * </ol>
     *
     * @param knowledgeBaseId 目标知识库ID
     * @param file            上传的文件
     * @return 文档上传响应，包含文档ID和状态
     * @throws BaseException 文件为空、格式不支持或上传失败时抛出对应异常
     */
    @Transactional(rollbackFor = Exception.class)
    public DocumentUploadResp uploadDocument(Long knowledgeBaseId, MultipartFile file) {
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
        documentMapper.insert(document);

        log.info("文档上传成功: id={}, title={}, knowledgeBaseId={}, path={}, status={}",
                document.getId(), originalFileName, knowledgeBaseId, filePath,
                DocumentStatus.PENDING.getCode());

        eventPublisher.publishEvent(new DocumentUploadedEvent(
                this, document.getId(), knowledgeBaseId, filePath, originalFileName));

        return new DocumentUploadResp(document.getId(), DocumentStatus.PENDING.getCode());
    }

    // ======================== 知识库管理 ========================

    /**
     * 创建知识库
     * <p>根据请求参数创建新的知识库记录，名称在系统内全局唯一</p>
     *
     * @param req 创建请求，包含名称和可选描述
     * @return 新创建的知识库ID
     * @throws BaseException 知识库名称已存在时抛出 KNOWLEDGE_BASE_NOT_EXIST
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createKnowledgeBase(CreateKnowledgeBaseReq req) {
        KnowledgeBaseDO kb = new KnowledgeBaseDO();
        kb.setName(req.name());
        kb.setDescription(req.description());
        knowledgeBaseMapper.insert(kb);
        log.info("知识库创建成功: id={}, name={}", kb.getId(), req.name());
        return kb.getId();
    }

    /**
     * 分页查询知识库列表
     * <p>支持按关键字模糊搜索名称和描述，返回分页结果并附带各知识库的文档数量统计</p>
     *
     * @param page     当前页码，从 1 开始
     * @param pageSize 每页记录数
     * @param keyword  搜索关键字，可选，模糊匹配名称和描述
     * @return 分页响应，包含知识库列表和文档数量
     */
    public PageResponse<KnowledgeBaseResp> listKnowledgeBases(long page, long pageSize, String keyword) {
        LambdaQueryWrapper<KnowledgeBaseDO> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(KnowledgeBaseDO::getName, keyword)
                    .or()
                    .like(KnowledgeBaseDO::getDescription, keyword));
        }
        wrapper.orderByDesc(KnowledgeBaseDO::getCreateTime);

        IPage<KnowledgeBaseDO> resultPage = knowledgeBaseMapper.selectPage(
                new Page<>(page, pageSize), wrapper);

        List<KnowledgeBaseDO> records = resultPage.getRecords();
        // 批量查询各知识库的文档数量
        Map<Long, Long> docCountMap = loadDocumentCounts(records);

        List<KnowledgeBaseResp> items = records.stream()
                .map(kb -> toKnowledgeBaseResp(kb, docCountMap.getOrDefault(kb.getId(), 0L)))
                .collect(Collectors.toList());

        return new PageResponse<>(items, resultPage.getTotal(), resultPage.getCurrent(), resultPage.getSize());
    }

    /**
     * 查询单个知识库详情
     *
     * @param id 知识库ID
     * @return 知识库响应 DTO，包含文档数量
     * @throws BaseException 知识库不存在时抛出 KNOWLEDGE_BASE_NOT_EXIST
     */
    public KnowledgeBaseResp getKnowledgeBase(Long id) {
        KnowledgeBaseDO kb = findKnowledgeBaseById(id);
        long docCount = documentMapper.selectCount(
                new LambdaQueryWrapper<DocumentDO>().eq(DocumentDO::getKnowledgeBaseId, id));
        return toKnowledgeBaseResp(kb, docCount);
    }

    /**
     * 更新知识库
     * <p>仅更新请求中非空字段，未传字段保持原值不变</p>
     *
     * @param id  知识库ID
     * @param req 更新请求，包含可选名称和描述
     * @throws BaseException 知识库不存在时抛出 KNOWLEDGE_BASE_NOT_EXIST
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateKnowledgeBase(Long id, UpdateKnowledgeBaseReq req) {
        KnowledgeBaseDO kb = findKnowledgeBaseById(id);
        if (StringUtils.hasText(req.name())) {
            kb.setName(req.name());
        }
        if (req.description() != null) {
            kb.setDescription(req.description());
        }
        knowledgeBaseMapper.updateById(kb);
        log.info("知识库已更新: id={}, name={}", id, kb.getName());
    }

    /**
     * 删除知识库
     * <p>删除知识库本身（当前不做级联删除文档处理，后续迭代可增加）</p>
     *
     * @param id 知识库ID
     * @throws BaseException 知识库不存在时抛出 KNOWLEDGE_BASE_NOT_EXIST
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteKnowledgeBase(Long id) {
        KnowledgeBaseDO kb = findKnowledgeBaseById(id);
        knowledgeBaseMapper.deleteById(id);
        log.info("知识库已删除: id={}, name={}", id, kb.getName());
    }

    /**
     * 根据ID查询知识库，不存在时抛出异常
     */
    private KnowledgeBaseDO findKnowledgeBaseById(Long id) {
        KnowledgeBaseDO kb = knowledgeBaseMapper.selectById(id);
        if (kb == null) {
            log.warn("知识库不存在: id={}", id);
            throw new BaseException(ErrorCodeEnum.KNOWLEDGE_BASE_NOT_EXIST);
        }
        return kb;
    }

    /**
     * 批量查询各知识库的文档数量
     */
    private Map<Long, Long> loadDocumentCounts(List<KnowledgeBaseDO> knowledgeBases) {
        if (knowledgeBases.isEmpty()) {
            return Map.of();
        }
        List<Long> kbIds = knowledgeBases.stream().map(KnowledgeBaseDO::getId).toList();
        List<DocumentDO> docs = documentMapper.selectList(
                new LambdaQueryWrapper<DocumentDO>().in(DocumentDO::getKnowledgeBaseId, kbIds));
        return docs.stream()
                .collect(Collectors.groupingBy(DocumentDO::getKnowledgeBaseId, Collectors.counting()));
    }

    /**
     * 将 DO 转换为响应 DTO
     */
    private KnowledgeBaseResp toKnowledgeBaseResp(KnowledgeBaseDO kb, long documentCount) {
        return new KnowledgeBaseResp(
                kb.getId(),
                kb.getName(),
                kb.getDescription(),
                documentCount,
                "completed",
                kb.getEmbeddingModelId(),
                kb.getEmbeddingDimension(),
                kb.getEmbeddingVersion(),
                kb.getCreateTime(),
                kb.getUpdateTime()
        );
    }

    // ======================== 文档管理 ========================

    /**
     * 分页查询文档列表
     * <p>支持按知识库ID过滤，返回分页结果。后续可扩展支持关键字搜索、
     * 状态过滤等查询条件。响应 DTO 中已预留 preview/download 相关字段</p>
     *
     * @param knowledgeBaseId 知识库ID，可选；传入时仅返回该知识库下的文档
     * @param page            当前页码，从 1 开始
     * @param pageSize        每页记录数
     * @return 分页响应，包含文档列表
     */
    public PageResponse<DocumentResp> listDocuments(Long knowledgeBaseId, long page, long pageSize) {
        LambdaQueryWrapper<DocumentDO> wrapper = new LambdaQueryWrapper<>();
        if (knowledgeBaseId != null) {
            wrapper.eq(DocumentDO::getKnowledgeBaseId, knowledgeBaseId);
        }
        wrapper.orderByDesc(DocumentDO::getCreateTime);

        IPage<DocumentDO> resultPage = documentMapper.selectPage(new Page<>(page, pageSize), wrapper);

        List<DocumentResp> items = resultPage.getRecords().stream()
                .map(this::toDocumentResp)
                .collect(Collectors.toList());

        return new PageResponse<>(items, resultPage.getTotal(), resultPage.getCurrent(), resultPage.getSize());
    }

    /**
     * 查询文档详情（含文件内容）
     * <p>根据文档ID查询文档元数据，同时从 MinIO 读取原始文件内容。
     * 适用于文档内容浏览页面，返回文档标题、文本内容、创建时间等完整信息。
     * 对于大文件或非文本文件，后续版本可增加分页读取和格式转换能力。</p>
     *
     * @param documentId 文档ID
     * @return 文档详情响应，包含元数据和文本内容
     * @throws BaseException 文档不存在时抛出 DOCUMENT_NOT_EXIST，文件读取失败时抛出 FILE_UPLOAD_FAILED
     */
    public DocumentDetailResp getDocumentDetail(Long documentId) {
        DocumentDO document = findDocumentById(documentId);

        String content = "";
        if (document.getFilePath() != null && !document.getFilePath().isBlank()) {
            content = minioStorageService.readFileContent(document.getFilePath());
        }

        String status = resolveStatus(document);
        String uploadedBy = document.getUploaderId() != null ? document.getUploaderId().toString() : "";

        return new DocumentDetailResp(
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
        return new DocumentStatusResp(resolveStatus(document));
    }

    /**
     * 解析文档状态
     * <p>兼容旧数据：若 status 字段为 null 或空，根据 filePath 是否存在回退判断</p>
     */
    private String resolveStatus(DocumentDO document) {
        if (document.getStatus() != null && !document.getStatus().isBlank()) {
            return document.getStatus();
        }
        return document.getFilePath() != null ? "uploaded" : "pending";
    }

    /**
     * 删除文档
     * <p>级联删除流程：
     * <ol>
     *   <li>查询该文档的所有块 chunkId</li>
     *   <li>删除 embedding_index（按 chunkId 精确删除，不影响其他文档）</li>
     *   <li>删除 document_chunk（按 documentId）</li>
     *   <li>删除 document（主表记录）</li>
     *   <li>尝试删除 MinIO 原文件（失败不影响业务）</li>
     * </ol>
     * </p>
     *
     * @param documentId 要删除的文档ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteDocument(Long documentId) {
        DocumentDO document = findDocumentById(documentId);

        List<Long> chunkIds = documentChunkMapper.selectList(
                        new LambdaQueryWrapper<DocumentChunkDO>()
                                .select(DocumentChunkDO::getId)
                                .eq(DocumentChunkDO::getDocumentId, documentId))
                .stream()
                .map(DocumentChunkDO::getId)
                .toList();

        if (!chunkIds.isEmpty()) {
            embeddingIndexStorageService.deleteByChunkIdsPgVector(chunkIds);
            log.info("文档关联向量索引已删除: documentId={}, chunkCount={}", documentId, chunkIds.size());
        }

        long chunkCount = documentChunkMapper.delete(
                new LambdaQueryWrapper<DocumentChunkDO>()
                        .eq(DocumentChunkDO::getDocumentId, documentId));
        log.info("文档关联块已删除: documentId={}, chunkCount={}", documentId, chunkCount);

        documentMapper.deleteById(documentId);
        log.info("文档记录已删除: id={}, title={}, knowledgeBaseId={}",
                documentId, document.getTitle(), document.getKnowledgeBaseId());

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
     * 将 DocumentDO 转换为 DocumentResp
     * <p>未来可在此方法中追加预览链接、下载链接等信息</p>
     */
    private DocumentResp toDocumentResp(DocumentDO doc) {
        String status = resolveStatus(doc);
        String uploadedBy = doc.getUploaderId() != null ? doc.getUploaderId().toString() : "";
        return new DocumentResp(
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
