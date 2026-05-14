package com.fukang.knowledge.agent.api.knowledgebase;

import com.fukang.knowledge.agent.api.knowledgebase.dto.CreateKnowledgeBaseReq;
import com.fukang.knowledge.agent.api.knowledgebase.dto.KnowledgeBaseResp;
import com.fukang.knowledge.agent.api.knowledgebase.dto.UpdateKnowledgeBaseReq;
import com.fukang.knowledge.agent.application.knowledge.KnowledgeBaseAppService;
import com.fukang.knowledge.agent.common.result.PageResponse;
import com.fukang.knowledge.agent.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 知识库管理控制器
 * <p>提供知识库的完整 CRUD REST API，包括创建、分页查询、
 * 详情查询、更新和删除。支持关键字搜索和文档数量统计</p>
 */
@RestController
@RequestMapping("/api/knowledge-bases")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseAppService knowledgeBaseAppService;

    /**
     * 创建知识库
     *
     * @param req 创建请求参数，name 必填
     * @return 新创建的知识库ID
     */
    @PostMapping
    public Result<Long> createKnowledgeBase(@RequestBody @Validated CreateKnowledgeBaseReq req) {
        return Result.success(knowledgeBaseAppService.createKnowledgeBase(req));
    }

    /**
     * 分页查询知识库列表
     * <p>支持关键字搜索，模糊匹配知识库名称和描述。按创建时间倒序排列。
     *
     * @param page     页码，从 1 开始，默认 1
     * @param pageSize 每页条数，默认 20
     * @param keyword  搜索关键字，可选
     * @return 分页响应，包含知识库列表、文档数量和分页信息
     */
    @GetMapping
    public Result<PageResponse<KnowledgeBaseResp>> listKnowledgeBases(
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "pageSize", defaultValue = "20") long pageSize,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return Result.success(knowledgeBaseAppService.listKnowledgeBases(page, pageSize, keyword));
    }

    /**
     * 查询知识库详情
     *
     * @param id 知识库ID
     * @return 知识库详细信息，包含文档数量
     */
    @GetMapping("/{id}")
    public Result<KnowledgeBaseResp> getKnowledgeBase(@PathVariable("id") Long id) {
        return Result.success(knowledgeBaseAppService.getKnowledgeBase(id));
    }

    /**
     * 更新知识库
     * <p>仅更新请求中非空字段，未传字段保持原值不变
     *
     * @param id  知识库ID
     * @param req 更新请求参数
     * @return 空成功响应
     */
    @PutMapping("/{id}")
    public Result<Void> updateKnowledgeBase(
            @PathVariable("id") Long id,
            @RequestBody @Validated UpdateKnowledgeBaseReq req) {
        knowledgeBaseAppService.updateKnowledgeBase(id, req);
        return Result.success();
    }

    /**
     * 删除知识库
     *
     * @param id 知识库ID
     * @return 空成功响应
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteKnowledgeBase(@PathVariable("id") Long id) {
        knowledgeBaseAppService.deleteKnowledgeBase(id);
        return Result.success();
    }
}