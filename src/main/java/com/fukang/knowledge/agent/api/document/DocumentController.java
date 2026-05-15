package com.fukang.knowledge.agent.api.document;

import com.fukang.knowledge.agent.api.document.dto.DocumentDetailResp;
import com.fukang.knowledge.agent.api.document.dto.DocumentResp;
import com.fukang.knowledge.agent.api.document.dto.DocumentStatusResp;
import com.fukang.knowledge.agent.api.document.dto.DocumentUploadResp;
import com.fukang.knowledge.agent.application.knowledge.KnowledgeBaseAppService;
import com.fukang.knowledge.agent.common.result.PageResponse;
import com.fukang.knowledge.agent.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文档管理控制器
 * <p>提供文档上传、列表查询、详情浏览、状态查询、删除等 REST API 接口</p>
 */
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final KnowledgeBaseAppService knowledgeBaseAppService;

    /**
     * 上传文档
     * <p>接收 Multipart 文件上传请求，将文件存入 MinIO 对象存储，
     * 并在数据库中创建文档记录。返回新创建的文档ID和状态。
     *
     * @param knowledgeBaseId 目标知识库ID，不能为空
     * @param file            上传的文件，通过 multipart/form-data 提交
     * @return 文档上传响应，包含 documentId 和 status
     */
    @PostMapping("/upload")
    public Result<DocumentUploadResp> uploadDocument(
            @RequestParam("knowledgeBaseId") Long knowledgeBaseId,
            @RequestPart("file") MultipartFile file) {
        DocumentUploadResp resp = knowledgeBaseAppService.uploadDocument(knowledgeBaseId, file);
        return Result.success(resp);
    }

    /**
     * 分页查询文档列表
     * <p>支持按知识库ID过滤，用于知识库详情页的文档列表展示。
     * 后续扩展方向：
     * <ul>
     *   <li>新增关键字搜索、状态筛选等查询条件</li>
     *   <li>新增 {@code GET /api/documents/{id}/preview} 文档预览接口</li>
     *   <li>新增 {@code GET /api/documents/{id}/download} 文档下载接口</li>
     * </ul>
     *
     * @param knowledgeBaseId 知识库ID，可选；传入时仅返回该知识库下的文档
     * @param page            页码，从 1 开始，默认 1
     * @param pageSize        每页条数，默认 20
     * @return 分页响应，包含文档列表
     */
    @GetMapping
    public Result<PageResponse<DocumentResp>> listDocuments(
            @RequestParam(value = "knowledgeBaseId", required = false) Long knowledgeBaseId,
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "pageSize", defaultValue = "20") long pageSize) {
        return Result.success(knowledgeBaseAppService.listDocuments(knowledgeBaseId, page, pageSize));
    }

    /**
     * 查询文档详情（含内容）
     * <p>根据文档ID查询文档完整信息，包括元数据和从 MinIO 读取的原始文件内容。
     * 用于文档内容浏览页面，展示文档标题、文本内容、创建时间、更新时间等完整信息。</p>
     *
     * @param id 文档ID
     * @return 文档详情响应，包含元数据和文本内容
     */
    @GetMapping("/{id}/detail")
    public Result<DocumentDetailResp> getDocumentDetail(@PathVariable("id") Long id) {
        return Result.success(knowledgeBaseAppService.getDocumentDetail(id));
    }

    /**
     * 查询文档处理状态
     * <p>根据文档ID查询文档当前的入库处理阶段，前端可用于轮询展示上传进度。
     *
     * @param id 文档ID
     * @return 文档状态响应，包含状态标识（如 pending、uploaded、processing、completed、failed）
     */
    @GetMapping("/{id}/status")
    public Result<DocumentStatusResp> getDocumentStatus(@PathVariable("id") Long id) {
        DocumentStatusResp resp = knowledgeBaseAppService.getDocumentStatus(id);
        return Result.success(resp);
    }

    /**
     * 删除文档
     * <p>根据文档ID删除文档记录，同时清理 MinIO 中存储的原始文件。
     *
     * @param id 文档ID
     * @return 空成功响应
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteDocument(@PathVariable("id") Long id) {
        knowledgeBaseAppService.deleteDocument(id);
        return Result.success();
    }
}