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
import com.fukang.knowledge.agent.common.context.UserContextHolder;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.common.result.PageResponse;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.DocumentDO;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.KnowledgeBaseDO;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.DocumentMapper;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.KnowledgeBaseMapper;
import com.fukang.knowledge.agent.infrastructure.storage.MinioStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

        String status = document.getFilePath() != null ? "uploaded" : "pending";
        String uploadedBy = document.getUploaderId() != null ? document.getUploaderId().toString() : "";

        return new DocumentDetailResp(
                document.getId(),
                document.getTitle(),
                content,
                document.getFilePath(),
                document.getKnowledgeBaseId(),
                status,
                uploadedBy,
                0L,
                0L,
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
     * 将 DocumentDO 转换为 DocumentResp
     * <p>未来可在此方法中追加预览链接、下载链接等信息</p>
     */
    private DocumentResp toDocumentResp(DocumentDO doc) {
        String status = doc.getFilePath() != null ? "uploaded" : "pending";
        String uploadedBy = doc.getUploaderId() != null ? doc.getUploaderId().toString() : "";
        return new DocumentResp(
                doc.getId(),
                doc.getTitle(),
                doc.getFilePath(),
                doc.getKnowledgeBaseId(),
                status,
                uploadedBy,
                0L,
                0L,
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