package com.fukang.knowledge.agent.api.evaluation;

import com.fukang.knowledge.agent.api.evaluation.dto.EvaluationCaseReq;
import com.fukang.knowledge.agent.api.evaluation.dto.EvaluationCaseResp;
import com.fukang.knowledge.agent.api.evaluation.dto.EvaluationCaseResultResp;
import com.fukang.knowledge.agent.api.evaluation.dto.EvaluationChunkResp;
import com.fukang.knowledge.agent.api.evaluation.dto.EvaluationDatasetReq;
import com.fukang.knowledge.agent.api.evaluation.dto.EvaluationDatasetResp;
import com.fukang.knowledge.agent.api.evaluation.dto.EvaluationRunCreateResp;
import com.fukang.knowledge.agent.api.evaluation.dto.EvaluationRunResp;
import com.fukang.knowledge.agent.application.evaluation.EvaluationAppService;
import com.fukang.knowledge.agent.application.evaluation.command.CreateEvaluationDatasetCommand;
import com.fukang.knowledge.agent.application.evaluation.command.SaveEvaluationCaseCommand;
import com.fukang.knowledge.agent.application.evaluation.command.UpdateEvaluationDatasetCommand;
import com.fukang.knowledge.agent.application.evaluation.result.EvaluationCaseResult;
import com.fukang.knowledge.agent.application.evaluation.result.EvaluationCaseRunResult;
import com.fukang.knowledge.agent.application.evaluation.result.EvaluationChunkResult;
import com.fukang.knowledge.agent.application.evaluation.result.EvaluationDatasetResult;
import com.fukang.knowledge.agent.application.evaluation.result.EvaluationRunResult;
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

    private final EvaluationAppService evaluationAppService;

    @GetMapping("/datasets")
    public Result<PageResponse<EvaluationDatasetResp>> listDatasets(
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "pageSize", defaultValue = "20") long pageSize,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "knowledgeBaseId", required = false) Long knowledgeBaseId) {
        PageResponse<EvaluationDatasetResult> datasets =
                evaluationAppService.listDatasets(page, pageSize, keyword, knowledgeBaseId);
        return Result.success(new PageResponse<>(
                datasets.getItems().stream().map(this::toDatasetResp).toList(),
                datasets.getTotal(), datasets.getPage(), datasets.getPageSize()));
    }

    @PostMapping("/datasets")
    public Result<Long> createDataset(@RequestBody EvaluationDatasetReq req) {
        Long id = evaluationAppService.createDataset(
                new CreateEvaluationDatasetCommand(req.name(), req.description(), req.knowledgeBaseId()));
        return Result.success(id);
    }

    @GetMapping("/datasets/{id}")
    public Result<EvaluationDatasetResp> getDataset(@PathVariable("id") Long id) {
        return Result.success(toDatasetResp(evaluationAppService.getDataset(id)));
    }

    @PutMapping("/datasets/{id}")
    public Result<Void> updateDataset(@PathVariable("id") Long id, @RequestBody EvaluationDatasetReq req) {
        evaluationAppService.updateDataset(id,
                new UpdateEvaluationDatasetCommand(req.name(), req.description(), req.knowledgeBaseId()));
        return Result.success();
    }

    @DeleteMapping("/datasets/{id}")
    public Result<Void> deleteDataset(@PathVariable("id") Long id) {
        evaluationAppService.deleteDataset(id);
        return Result.success();
    }

    @GetMapping("/datasets/{id}/cases")
    public Result<PageResponse<EvaluationCaseResp>> listCases(
            @PathVariable("id") Long datasetId,
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "pageSize", defaultValue = "20") long pageSize) {
        PageResponse<EvaluationCaseResult> cases = evaluationAppService.listCases(datasetId, page, pageSize);
        return Result.success(new PageResponse<>(
                cases.getItems().stream().map(this::toCaseResp).toList(),
                cases.getTotal(), cases.getPage(), cases.getPageSize()));
    }

    @PostMapping("/datasets/{id}/cases")
    public Result<Long> createCase(@PathVariable("id") Long datasetId, @RequestBody EvaluationCaseReq req) {
        return Result.success(evaluationAppService.createCase(datasetId, toCaseCommand(req)));
    }

    @PutMapping("/cases/{id}")
    public Result<Void> updateCase(@PathVariable("id") Long id, @RequestBody EvaluationCaseReq req) {
        evaluationAppService.updateCase(id, toCaseCommand(req));
        return Result.success();
    }

    @DeleteMapping("/cases/{id}")
    public Result<Void> deleteCase(@PathVariable("id") Long id) {
        evaluationAppService.deleteCase(id);
        return Result.success();
    }

    @PostMapping("/datasets/{id}/runs")
    public Result<EvaluationRunCreateResp> runDataset(@PathVariable("id") Long datasetId) {
        return Result.success(new EvaluationRunCreateResp(evaluationAppService.runDataset(datasetId)));
    }

    @GetMapping("/runs/{runId}")
    public Result<EvaluationRunResp> getRun(@PathVariable("runId") Long runId) {
        return Result.success(toRunResp(evaluationAppService.getRun(runId)));
    }

    @GetMapping("/runs/{runId}/results")
    public Result<PageResponse<EvaluationCaseResultResp>> listRunResults(
            @PathVariable("runId") Long runId,
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "pageSize", defaultValue = "20") long pageSize) {
        PageResponse<EvaluationCaseRunResult> results = evaluationAppService.listRunResults(runId, page, pageSize);
        return Result.success(new PageResponse<>(
                results.getItems().stream().map(this::toCaseResultResp).toList(),
                results.getTotal(), results.getPage(), results.getPageSize()));
    }

    private SaveEvaluationCaseCommand toCaseCommand(EvaluationCaseReq req) {
        return new SaveEvaluationCaseCommand(req.question(), req.expectedAnswer(), req.expectedKeywords(),
                req.expectedChunkIds(), req.expectedStatus(), req.metadata(), req.enabled());
    }

    private EvaluationDatasetResp toDatasetResp(EvaluationDatasetResult result) {
        return new EvaluationDatasetResp(result.id(), result.name(), result.description(), result.knowledgeBaseId(),
                result.targetType(), result.caseCount(), result.lastRunId(), result.lastRunStatus(),
                result.lastAvgScore(), result.createTime(), result.updateTime());
    }

    private EvaluationCaseResp toCaseResp(EvaluationCaseResult result) {
        return new EvaluationCaseResp(result.id(), result.datasetId(), result.question(), result.expectedAnswer(),
                result.expectedKeywords(), result.expectedChunkIds(), result.expectedStatus(), result.metadata(),
                result.enabled(), result.createTime(), result.updateTime());
    }

    private EvaluationRunResp toRunResp(EvaluationRunResult result) {
        return new EvaluationRunResp(result.id(), result.datasetId(), result.name(), result.targetType(),
                result.status(), result.totalCount(), result.passedCount(), result.failedCount(), result.avgScore(),
                result.avgLatencyMs(), result.startedAt(), result.endedAt(), result.errorMessage(),
                result.createTime(), result.updateTime());
    }

    private EvaluationCaseResultResp toCaseResultResp(EvaluationCaseRunResult result) {
        return new EvaluationCaseResultResp(result.id(), result.runId(), result.caseId(), result.question(),
                result.expectedAnswer(), result.actualAnswer(), result.rewrittenQuery(), result.expectedStatus(),
                result.actualStatus(), result.expectedKeywords(), result.expectedChunkIds(),
                result.retrievedChunks().stream().map(this::toChunkResp).toList(),
                result.rerankedChunks().stream().map(this::toChunkResp).toList(),
                result.retrievalHitScore(), result.keywordScore(), result.statusScore(), result.totalScore(),
                result.passed(), result.metricDetail(), result.latencyMs(), result.errorMessage(), result.createTime());
    }

    private EvaluationChunkResp toChunkResp(EvaluationChunkResult result) {
        return new EvaluationChunkResp(result.chunkId(), result.chunkText(), result.similarity(), result.metadata(),
                result.vectorScore(), result.bm25Score(), result.rrfScore(), result.rerankScore());
    }
}
