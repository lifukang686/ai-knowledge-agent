package com.fukang.knowledge.agent.api.document;

import com.fukang.knowledge.agent.api.document.dto.DocumentStatusResp;
import com.fukang.knowledge.agent.api.document.dto.DocumentUploadResp;
import com.fukang.knowledge.agent.application.knowledge.KnowledgeBaseAppService;
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
 * <p>提供文档上传、状态查询等 REST API 接口</p>
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