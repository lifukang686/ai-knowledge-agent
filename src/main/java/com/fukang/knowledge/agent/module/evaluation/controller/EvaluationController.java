package com.fukang.knowledge.agent.module.evaluation.controller;

import com.fukang.knowledge.agent.module.evaluation.model.dto.EvaluationCaseReq;
import com.fukang.knowledge.agent.module.evaluation.model.resp.EvaluationCaseResp;
import com.fukang.knowledge.agent.module.evaluation.model.resp.EvaluationCaseResultResp;
import com.fukang.knowledge.agent.module.evaluation.model.resp.EvaluationChunkResp;
import com.fukang.knowledge.agent.module.evaluation.model.dto.EvaluationDatasetReq;
import com.fukang.knowledge.agent.module.evaluation.model.resp.EvaluationDatasetResp;
import com.fukang.knowledge.agent.module.evaluation.model.resp.EvaluationRunCreateResp;
import com.fukang.knowledge.agent.module.evaluation.model.resp.EvaluationRunResp;
import com.fukang.knowledge.agent.module.evaluation.service.EvaluationService;
import com.fukang.knowledge.agent.module.evaluation.model.dto.CreateEvaluationDatasetCommand;
import com.fukang.knowledge.agent.module.evaluation.model.dto.SaveEvaluationCaseCommand;
import com.fukang.knowledge.agent.module.evaluation.model.dto.UpdateEvaluationDatasetCommand;
import com.fukang.knowledge.agent.module.evaluation.model.vo.EvaluationCaseResult;
import com.fukang.knowledge.agent.module.evaluation.model.vo.EvaluationCaseRunResult;
import com.fukang.knowledge.agent.module.evaluation.model.vo.EvaluationChunkResult;
import com.fukang.knowledge.agent.module.evaluation.model.vo.EvaluationDatasetResult;
import com.fukang.knowledge.agent.module.evaluation.model.vo.EvaluationRunResult;
import com.fukang.knowledge.agent.common.result.PageResponse;
import com.fukang.knowledge.agent.common.result.Result;
import lombok.RequiredArgsConstructor;
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
 * RAG 评测中心接口。
 */
@RestController
@RequestMapping("/api/evaluations")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;

    /**
     * 分页查询评测集。
     */
    @GetMapping("/datasets")
    public Result<PageResponse<EvaluationDatasetResp>> listDatasets(
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "pageSize", defaultValue = "20") long pageSize,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "knowledgeBaseId", required = false) Long knowledgeBaseId) {
        PageResponse<EvaluationDatasetResult> datasets =
                evaluationService.listDatasets(page, pageSize, keyword, knowledgeBaseId);
        return Result.success(new PageResponse<>(
                datasets.getItems().stream().map(this::toDatasetResp).toList(),
                datasets.getTotal(), datasets.getPage(), datasets.getPageSize()));
    }

    /**
     * 创建评测集。
     */
    @PostMapping("/datasets")
    public Result<Long> createDataset(@RequestBody EvaluationDatasetReq req) {
        Long id = evaluationService.createDataset(
                new CreateEvaluationDatasetCommand(req.getName(), req.getDescription(), req.getKnowledgeBaseId()));
        return Result.success(id);
    }

    /**
     * 查询评测集详情。
     */
    @GetMapping("/datasets/{id}")
    public Result<EvaluationDatasetResp> getDataset(@PathVariable("id") Long id) {
        return Result.success(toDatasetResp(evaluationService.getDataset(id)));
    }

    /**
     * 更新评测集。
     */
    @PutMapping("/datasets/{id}")
    public Result<Void> updateDataset(@PathVariable("id") Long id, @RequestBody EvaluationDatasetReq req) {
        evaluationService.updateDataset(id,
                new UpdateEvaluationDatasetCommand(req.getName(), req.getDescription(), req.getKnowledgeBaseId()));
        return Result.success();
    }

    /**
     * 删除评测集。
     */
    @DeleteMapping("/datasets/{id}")
    public Result<Void> deleteDataset(@PathVariable("id") Long id) {
        evaluationService.deleteDataset(id);
        return Result.success();
    }

    /**
     * 分页查询评测用例。
     */
    @GetMapping("/datasets/{id}/cases")
    public Result<PageResponse<EvaluationCaseResp>> listCases(
            @PathVariable("id") Long datasetId,
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "pageSize", defaultValue = "20") long pageSize) {
        PageResponse<EvaluationCaseResult> cases = evaluationService.listCases(datasetId, page, pageSize);
        return Result.success(new PageResponse<>(
                cases.getItems().stream().map(this::toCaseResp).toList(),
                cases.getTotal(), cases.getPage(), cases.getPageSize()));
    }

    /**
     * 创建评测用例。
     */
    @PostMapping("/datasets/{id}/cases")
    public Result<Long> createCase(@PathVariable("id") Long datasetId, @RequestBody EvaluationCaseReq req) {
        return Result.success(evaluationService.createCase(datasetId, toCaseCommand(req)));
    }

    /**
     * 更新评测用例。
     */
    @PutMapping("/cases/{id}")
    public Result<Void> updateCase(@PathVariable("id") Long id, @RequestBody EvaluationCaseReq req) {
        evaluationService.updateCase(id, toCaseCommand(req));
        return Result.success();
    }

    /**
     * 删除评测用例。
     */
    @DeleteMapping("/cases/{id}")
    public Result<Void> deleteCase(@PathVariable("id") Long id) {
        evaluationService.deleteCase(id);
        return Result.success();
    }

    /**
     * 手动运行评测集。
     */
    @PostMapping("/datasets/{id}/runs")
    public Result<EvaluationRunCreateResp> runDataset(@PathVariable("id") Long datasetId) {
        return Result.success(new EvaluationRunCreateResp(evaluationService.runDataset(datasetId)));
    }

    /**
     * 查询运行汇总。
     */
    @GetMapping("/runs/{runId}")
    public Result<EvaluationRunResp> getRun(@PathVariable("runId") Long runId) {
        return Result.success(toRunResp(evaluationService.getRun(runId)));
    }

    /**
     * 分页查询运行明细。
     */
    @GetMapping("/runs/{runId}/results")
    public Result<PageResponse<EvaluationCaseResultResp>> listRunResults(
            @PathVariable("runId") Long runId,
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "pageSize", defaultValue = "20") long pageSize) {
        PageResponse<EvaluationCaseRunResult> results = evaluationService.listRunResults(runId, page, pageSize);
        return Result.success(new PageResponse<>(
                results.getItems().stream().map(this::toCaseResultResp).toList(),
                results.getTotal(), results.getPage(), results.getPageSize()));
    }

    /**
     * 请求 DTO 转应用命令。
     */
    private SaveEvaluationCaseCommand toCaseCommand(EvaluationCaseReq req) {
        return new SaveEvaluationCaseCommand(req.getQuestion(), req.getExpectedAnswer(), req.getExpectedKeywords(),
                req.getExpectedChunkIds(), req.getExpectedStatus(), req.getMetadata(), req.getEnabled());
    }

    /**
     * 应用结果转评测集响应。
     */
    private EvaluationDatasetResp toDatasetResp(EvaluationDatasetResult result) {
        return new EvaluationDatasetResp(result.getId(), result.getName(), result.getDescription(), result.getKnowledgeBaseId(),
                result.getTargetType(), result.getCaseCount(), result.getLastRunId(), result.getLastRunStatus(),
                result.getLastAvgScore(), result.getCreateTime(), result.getUpdateTime());
    }

    /**
     * 应用结果转用例响应。
     */
    private EvaluationCaseResp toCaseResp(EvaluationCaseResult result) {
        return new EvaluationCaseResp(result.getId(), result.getDatasetId(), result.getQuestion(), result.getExpectedAnswer(),
                result.getExpectedKeywords(), result.getExpectedChunkIds(), result.getExpectedStatus(), result.getMetadata(),
                result.getEnabled(), result.getCreateTime(), result.getUpdateTime());
    }

    /**
     * 应用结果转运行响应。
     */
    private EvaluationRunResp toRunResp(EvaluationRunResult result) {
        return new EvaluationRunResp(result.getId(), result.getDatasetId(), result.getName(), result.getTargetType(),
                result.getStatus(), result.getTotalCount(), result.getPassedCount(), result.getFailedCount(), result.getAvgScore(),
                result.getAvgLatencyMs(), result.getStartedAt(), result.getEndedAt(), result.getErrorMessage(),
                result.getCreateTime(), result.getUpdateTime());
    }

    /**
     * 应用结果转运行明细响应。
     */
    private EvaluationCaseResultResp toCaseResultResp(EvaluationCaseRunResult result) {
        return new EvaluationCaseResultResp(result.getId(), result.getRunId(), result.getCaseId(), result.getQuestion(),
                result.getExpectedAnswer(), result.getActualAnswer(), result.getRewrittenQuery(), result.getExpectedStatus(),
                result.getActualStatus(), result.getExpectedKeywords(), result.getExpectedChunkIds(),
                result.getRetrievedChunks().stream().map(this::toChunkResp).toList(),
                result.getRerankedChunks().stream().map(this::toChunkResp).toList(),
                result.getRetrievalHitScore(), result.getKeywordScore(), result.getStatusScore(), result.getTotalScore(),
                result.getPassed(), result.getMetricDetail(), result.getLatencyMs(), result.getErrorMessage(), result.getCreateTime());
    }

    /**
     * 应用片段结果转前端响应。
     */
    private EvaluationChunkResp toChunkResp(EvaluationChunkResult result) {
        return new EvaluationChunkResp(result.getChunkId(), result.getChunkText(), result.getSimilarity(), result.getMetadata(),
                result.getVectorScore(), result.getBm25Score(), result.getRrfScore(), result.getRerankScore());
    }
}
