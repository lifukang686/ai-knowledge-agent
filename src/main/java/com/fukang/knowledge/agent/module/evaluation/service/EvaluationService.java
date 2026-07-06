package com.fukang.knowledge.agent.module.evaluation.service;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fukang.knowledge.agent.module.evaluation.model.dto.CreateEvaluationDatasetCommand;
import com.fukang.knowledge.agent.module.evaluation.model.dto.SaveEvaluationCaseCommand;
import com.fukang.knowledge.agent.module.evaluation.model.dto.UpdateEvaluationDatasetCommand;
import com.fukang.knowledge.agent.module.evaluation.model.vo.EvaluationCaseResult;
import com.fukang.knowledge.agent.module.evaluation.model.vo.EvaluationCaseRunResult;
import com.fukang.knowledge.agent.module.evaluation.model.vo.EvaluationChunkResult;
import com.fukang.knowledge.agent.module.evaluation.model.vo.EvaluationDatasetResult;
import com.fukang.knowledge.agent.module.evaluation.model.vo.EvaluationRunResult;
import com.fukang.knowledge.agent.module.knowledge.mapper.KnowledgeBaseMapper;
import com.fukang.knowledge.agent.module.rag.service.RagService;
import com.fukang.knowledge.agent.module.rag.model.vo.RagEvalResult;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.common.result.PageResponse;
import com.fukang.knowledge.agent.module.rag.model.vo.SearchResult;
import com.fukang.knowledge.agent.module.evaluation.model.entity.EvaluationCaseEntity;
import com.fukang.knowledge.agent.module.evaluation.model.entity.EvaluationCaseResultRecordEntity;
import com.fukang.knowledge.agent.module.evaluation.model.entity.EvaluationDatasetEntity;
import com.fukang.knowledge.agent.module.evaluation.model.entity.EvaluationRunEntity;
import com.fukang.knowledge.agent.module.evaluation.mapper.EvaluationCaseMapper;
import com.fukang.knowledge.agent.module.evaluation.mapper.EvaluationCaseResultMapper;
import com.fukang.knowledge.agent.module.evaluation.mapper.EvaluationDatasetMapper;
import com.fukang.knowledge.agent.module.evaluation.mapper.EvaluationRunMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * RAG 评测应用服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationService {

    /**
     * 当前评测对象仅覆盖 RAG 问答。
     */
    private static final String TARGET_TYPE_RAG_QA = "RAG_QA";
    /**
     * 评测运行中。
     */
    private static final String STATUS_RUNNING = "RUNNING";
    /**
     * 评测已完成。
     */
    private static final String STATUS_COMPLETED = "COMPLETED";
    /**
     * 评测执行失败。
     */
    private static final String STATUS_FAILED = "FAILED";
    /**
     * 用例通过阈值。
     */
    private static final double PASS_THRESHOLD = 70.0D;

    private final EvaluationDatasetMapper datasetMapper;
    private final EvaluationCaseMapper caseMapper;
    private final EvaluationRunMapper runMapper;
    private final EvaluationCaseResultMapper caseResultMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final RagService ragService;
    private final ObjectMapper objectMapper;

    /**
     * 分页查询评测集。
     */
    public PageResponse<EvaluationDatasetResult> listDatasets(long page, long pageSize, String keyword,
                                                              Long knowledgeBaseId) {
        LambdaQueryWrapper<EvaluationDatasetEntity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(EvaluationDatasetEntity::getName, keyword)
                    .or()
                    .like(EvaluationDatasetEntity::getDescription, keyword));
        }
        if (knowledgeBaseId != null) {
            wrapper.eq(EvaluationDatasetEntity::getKnowledgeBaseId, knowledgeBaseId);
        }
        wrapper.orderByDesc(EvaluationDatasetEntity::getCreateTime);
        IPage<EvaluationDatasetEntity> resultPage = datasetMapper.selectPage(new Page<>(page, pageSize), wrapper);
        List<EvaluationDatasetResult> items = resultPage.getRecords().stream()
                .map(this::toDatasetResult)
                .toList();
        return new PageResponse<>(items, resultPage.getTotal(), resultPage.getCurrent(), resultPage.getSize());
    }

    /**
     * 创建 RAG 评测集。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createDataset(CreateEvaluationDatasetCommand command) {
        validateDataset(command.getName(), command.getKnowledgeBaseId());
        EvaluationDatasetEntity dataset = new EvaluationDatasetEntity();
        dataset.setName(command.getName().trim());
        dataset.setDescription(trimToNull(command.getDescription()));
        dataset.setKnowledgeBaseId(command.getKnowledgeBaseId());
        dataset.setTargetType(TARGET_TYPE_RAG_QA);
        datasetMapper.insert(dataset);
        return dataset.getId();
    }

    /**
     * 查询评测集详情。
     */
    public EvaluationDatasetResult getDataset(Long id) {
        return toDatasetResult(findDataset(id));
    }

    /**
     * 更新评测集基础信息。
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateDataset(Long id, UpdateEvaluationDatasetCommand command) {
        EvaluationDatasetEntity dataset = findDataset(id);
        validateDataset(command.getName(), command.getKnowledgeBaseId());
        dataset.setName(command.getName().trim());
        dataset.setDescription(trimToNull(command.getDescription()));
        dataset.setKnowledgeBaseId(command.getKnowledgeBaseId());
        datasetMapper.updateById(dataset);
    }

    /**
     * 删除评测集及其用例、运行和结果。
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteDataset(Long id) {
        findDataset(id);
        // 先删除结果，再删运行和用例，避免残留孤儿数据。
        List<Long> runIds = runMapper.selectList(new LambdaQueryWrapper<EvaluationRunEntity>()
                        .select(EvaluationRunEntity::getId)
                        .eq(EvaluationRunEntity::getDatasetId, id))
                .stream()
                .map(EvaluationRunEntity::getId)
                .toList();
        if (!runIds.isEmpty()) {
            caseResultMapper.delete(new LambdaQueryWrapper<EvaluationCaseResultRecordEntity>()
                    .in(EvaluationCaseResultRecordEntity::getRunId, runIds));
        }
        runMapper.delete(new LambdaQueryWrapper<EvaluationRunEntity>().eq(EvaluationRunEntity::getDatasetId, id));
        caseMapper.delete(new LambdaQueryWrapper<EvaluationCaseEntity>().eq(EvaluationCaseEntity::getDatasetId, id));
        datasetMapper.deleteById(id);
    }

    /**
     * 删除知识库关联的评测集及历史结果。
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteDatasetsByKnowledgeBase(Long knowledgeBaseId) {
        List<Long> datasetIds = datasetMapper.selectList(new LambdaQueryWrapper<EvaluationDatasetEntity>()
                        .select(EvaluationDatasetEntity::getId)
                        .eq(EvaluationDatasetEntity::getKnowledgeBaseId, knowledgeBaseId))
                .stream()
                .map(EvaluationDatasetEntity::getId)
                .toList();
        for (Long datasetId : datasetIds) {
            deleteDataset(datasetId);
        }
    }

    /**
     * 分页查询评测用例。
     */
    public PageResponse<EvaluationCaseResult> listCases(Long datasetId, long page, long pageSize) {
        findDataset(datasetId);
        IPage<EvaluationCaseEntity> resultPage = caseMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<EvaluationCaseEntity>()
                        .eq(EvaluationCaseEntity::getDatasetId, datasetId)
                        .orderByDesc(EvaluationCaseEntity::getCreateTime));
        return new PageResponse<>(resultPage.getRecords().stream().map(this::toCaseResult).toList(),
                resultPage.getTotal(), resultPage.getCurrent(), resultPage.getSize());
    }

    /**
     * 创建评测用例。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createCase(Long datasetId, SaveEvaluationCaseCommand command) {
        findDataset(datasetId);
        validateCase(command);
        EvaluationCaseEntity testCase = new EvaluationCaseEntity();
        fillCase(testCase, datasetId, command);
        caseMapper.insert(testCase);
        return testCase.getId();
    }

    /**
     * 更新评测用例。
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateCase(Long id, SaveEvaluationCaseCommand command) {
        EvaluationCaseEntity testCase = findCase(id);
        validateCase(command);
        fillCase(testCase, testCase.getDatasetId(), command);
        caseMapper.updateById(testCase);
    }

    /**
     * 删除评测用例及历史结果。
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteCase(Long id) {
        findCase(id);
        caseResultMapper.delete(new LambdaQueryWrapper<EvaluationCaseResultRecordEntity>()
                .eq(EvaluationCaseResultRecordEntity::getCaseId, id));
        caseMapper.deleteById(id);
    }

    /**
     * 同步运行整个评测集。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long runDataset(Long datasetId) {
        EvaluationDatasetEntity dataset = findDataset(datasetId);
        EvaluationRunEntity run = createRun(dataset);
        List<EvaluationCaseEntity> cases = caseMapper.selectList(new LambdaQueryWrapper<EvaluationCaseEntity>()
                .eq(EvaluationCaseEntity::getDatasetId, datasetId)
                .eq(EvaluationCaseEntity::getEnabled, true)
                .orderByAsc(EvaluationCaseEntity::getCreateTime));

        if (cases.isEmpty()) {
            // 空评测集不产生用例结果，直接标记失败便于前端提示。
            run.setStatus(STATUS_FAILED);
            run.setTotalCount(0);
            run.setPassedCount(0);
            run.setFailedCount(0);
            run.setAvgScore(0.0D);
            run.setAvgLatencyMs(0L);
            run.setEndedAt(LocalDateTime.now());
            run.setErrorMessage("评测集暂无启用的用例");
            runMapper.updateById(run);
            return run.getId();
        }

        int passed = 0;
        double scoreSum = 0.0D;
        long latencySum = 0L;
        for (EvaluationCaseEntity testCase : cases) {
            // 每条用例独立捕获异常，避免单条失败中断整批评测。
            EvaluationCaseResultRecordEntity result = runCase(run.getId(), dataset.getKnowledgeBaseId(), testCase);
            caseResultMapper.insert(result);
            if (Boolean.TRUE.equals(result.getPassed())) {
                passed++;
            }
            scoreSum += result.getTotalScore() != null ? result.getTotalScore() : 0.0D;
            latencySum += result.getLatencyMs() != null ? result.getLatencyMs() : 0L;
        }

        run.setStatus(STATUS_COMPLETED);
        run.setTotalCount(cases.size());
        run.setPassedCount(passed);
        run.setFailedCount(cases.size() - passed);
        run.setAvgScore(round(scoreSum / cases.size()));
        run.setAvgLatencyMs(latencySum / cases.size());
        run.setEndedAt(LocalDateTime.now());
        runMapper.updateById(run);
        return run.getId();
    }

    /**
     * 查询评测运行汇总。
     */
    public EvaluationRunResult getRun(Long runId) {
        EvaluationRunEntity run = runMapper.selectById(runId);
        if (run == null) {
            throw new BaseException(ErrorCodeEnum.NOT_FOUND.getCode(), "评测运行记录不存在");
        }
        return toRunResult(run);
    }

    /**
     * 分页查询评测运行明细。
     */
    public PageResponse<EvaluationCaseRunResult> listRunResults(Long runId, long page, long pageSize) {
        getRun(runId);
        IPage<EvaluationCaseResultRecordEntity> resultPage = caseResultMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<EvaluationCaseResultRecordEntity>()
                        .eq(EvaluationCaseResultRecordEntity::getRunId, runId)
                        .orderByAsc(EvaluationCaseResultRecordEntity::getCreateTime));
        return new PageResponse<>(resultPage.getRecords().stream().map(this::toCaseRunResult).toList(),
                resultPage.getTotal(), resultPage.getCurrent(), resultPage.getSize());
    }

    /**
     * 初始化运行记录。
     */
    private EvaluationRunEntity createRun(EvaluationDatasetEntity dataset) {
        EvaluationRunEntity run = new EvaluationRunEntity();
        run.setDatasetId(dataset.getId());
        run.setName(dataset.getName() + " - " + LocalDateTime.now());
        run.setTargetType(TARGET_TYPE_RAG_QA);
        run.setStatus(STATUS_RUNNING);
        run.setTotalCount(0);
        run.setPassedCount(0);
        run.setFailedCount(0);
        run.setStartedAt(LocalDateTime.now());
        runMapper.insert(run);
        return run;
    }

    /**
     * 执行单条评测用例并落评分结果。
     */
    private EvaluationCaseResultRecordEntity runCase(Long runId, Long knowledgeBaseId, EvaluationCaseEntity testCase) {
        EvaluationCaseResultRecordEntity result = baseCaseResult(runId, testCase);
        try {
            RagEvalResult ragResult = ragService.evaluate(testCase.getQuestion(), knowledgeBaseId);
            Score score = score(testCase, ragResult);
            result.setActualAnswer(ragResult.getAnswer());
            result.setRewrittenQuery(ragResult.getRewrittenQuery());
            result.setActualStatus(ragResult.getStatus());
            // 保存检索 trace，便于结果页定位召回和重排问题。
            result.setRetrievedChunks(toJson(toChunkResults(ragResult.getRetrievedChunks())));
            result.setRerankedChunks(toJson(toChunkResults(ragResult.getRerankedChunks())));
            result.setRetrievalHitScore(score.getRetrievalHitScore());
            result.setKeywordScore(score.getKeywordScore());
            result.setStatusScore(score.getStatusScore());
            result.setTotalScore(score.getTotalScore());
            result.setPassed(score.getPassed());
            result.setMetricDetail(toJson(score.getDetail()));
            result.setLatencyMs(ragResult.getLatencyMs());
        } catch (Exception e) {
            log.warn("评测用例执行失败: caseId={}", testCase.getId(), e);
            result.setActualStatus("failed");
            result.setTotalScore(0.0D);
            result.setPassed(false);
            result.setMetricDetail(toJson(Map.of("error", e.getClass().getSimpleName())));
            result.setErrorMessage(e.getMessage());
        }
        return result;
    }

    /**
     * 构造用例结果快照，避免后续用例修改影响历史结果。
     */
    private EvaluationCaseResultRecordEntity baseCaseResult(Long runId, EvaluationCaseEntity testCase) {
        EvaluationCaseResultRecordEntity result = new EvaluationCaseResultRecordEntity();
        result.setRunId(runId);
        result.setCaseId(testCase.getId());
        result.setQuestion(testCase.getQuestion());
        result.setExpectedAnswer(testCase.getExpectedAnswer());
        result.setExpectedStatus(testCase.getExpectedStatus());
        result.setExpectedKeywords(testCase.getExpectedKeywords());
        result.setExpectedChunkIds(testCase.getExpectedChunkIds());
        result.setTotalScore(0.0D);
        result.setPassed(false);
        return result;
    }

    /**
     * 按已配置指标计算总分。
     */
    private Score score(EvaluationCaseEntity testCase, RagEvalResult ragResult) {
        List<Double> parts = new ArrayList<>();
        Map<String, Object> detail = new LinkedHashMap<>();

        List<Long> expectedChunkIds = parseLongList(testCase.getExpectedChunkIds());
        Double retrievalScore = null;
        if (!expectedChunkIds.isEmpty()) {
            // 期望 Chunk 命中原始召回或重排结果均算召回成功。
            boolean hit = hasChunkHit(expectedChunkIds, ragResult.getRetrievedChunks())
                    || hasChunkHit(expectedChunkIds, ragResult.getRerankedChunks());
            retrievalScore = hit ? 100.0D : 0.0D;
            parts.add(retrievalScore);
            detail.put("retrievalHit", hit);
        }

        List<String> keywords = parseStringList(testCase.getExpectedKeywords());
        Double keywordScore = null;
        if (!keywords.isEmpty()) {
            // 关键词按答案覆盖比例计分。
            long hitCount = keywords.stream().filter(keyword -> contains(ragResult.getAnswer(), keyword)).count();
            keywordScore = round(hitCount * 100.0D / keywords.size());
            parts.add(keywordScore);
            detail.put("keywordHitCount", hitCount);
            detail.put("keywordTotal", keywords.size());
        }

        Double statusScore = null;
        if (StringUtils.hasText(testCase.getExpectedStatus())) {
            // 状态用于校验 success/no_results 等链路结果。
            boolean match = testCase.getExpectedStatus().equalsIgnoreCase(
                    ragResult.getStatus() != null ? ragResult.getStatus() : "");
            statusScore = match ? 100.0D : 0.0D;
            parts.add(statusScore);
            detail.put("statusMatch", match);
        }

        double totalScore = parts.isEmpty()
                ? 100.0D
                : round(parts.stream().mapToDouble(Double::doubleValue).average().orElse(0.0D));
        boolean passed = totalScore >= PASS_THRESHOLD;
        detail.put("totalScore", totalScore);
        detail.put("passed", passed);
        return new Score(retrievalScore, keywordScore, statusScore, totalScore, passed, detail);
    }

    /**
     * 判断期望片段是否被召回。
     */
    private boolean hasChunkHit(List<Long> expectedChunkIds, List<SearchResult> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return false;
        }
        return chunks.stream().map(SearchResult::getChunkId).filter(Objects::nonNull).anyMatch(expectedChunkIds::contains);
    }

    /**
     * 忽略大小写判断答案是否包含关键词。
     */
    private boolean contains(String answer, String keyword) {
        if (!StringUtils.hasText(answer) || !StringUtils.hasText(keyword)) {
            return false;
        }
        return answer.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    /**
     * 校验评测集基础字段。
     */
    private void validateDataset(String name, Long knowledgeBaseId) {
        if (!StringUtils.hasText(name)) {
            throw new BaseException(ErrorCodeEnum.BAD_REQUEST.getCode(), "评测集名称不能为空");
        }
        if (knowledgeBaseId != null && knowledgeBaseMapper.selectById(knowledgeBaseId) == null) {
            throw new BaseException(ErrorCodeEnum.KNOWLEDGE_BASE_NOT_EXIST);
        }
    }

    /**
     * 校验评测用例基础字段。
     */
    private void validateCase(SaveEvaluationCaseCommand command) {
        if (command == null || !StringUtils.hasText(command.getQuestion())) {
            throw new BaseException(ErrorCodeEnum.BAD_REQUEST.getCode(), "评测问题不能为空");
        }
    }

    /**
     * 填充用例持久化字段。
     */
    private void fillCase(EvaluationCaseEntity testCase, Long datasetId, SaveEvaluationCaseCommand command) {
        testCase.setDatasetId(datasetId);
        testCase.setQuestion(command.getQuestion().trim());
        testCase.setExpectedAnswer(trimToNull(command.getExpectedAnswer()));
        testCase.setExpectedKeywords(toJson(command.getExpectedKeywords() != null ? command.getExpectedKeywords() : List.of()));
        testCase.setExpectedChunkIds(toJson(command.getExpectedChunkIds() != null ? command.getExpectedChunkIds() : List.of()));
        testCase.setExpectedStatus(trimToNull(command.getExpectedStatus()));
        testCase.setMetadata(trimToNull(command.getMetadata()));
        testCase.setEnabled(command.getEnabled() == null || command.getEnabled());
    }

    /**
     * 查询评测集，不存在时抛业务异常。
     */
    private EvaluationDatasetEntity findDataset(Long id) {
        EvaluationDatasetEntity dataset = datasetMapper.selectById(id);
        if (dataset == null) {
            throw new BaseException(ErrorCodeEnum.NOT_FOUND.getCode(), "评测集不存在");
        }
        return dataset;
    }

    /**
     * 查询评测用例，不存在时抛业务异常。
     */
    private EvaluationCaseEntity findCase(Long id) {
        EvaluationCaseEntity testCase = caseMapper.selectById(id);
        if (testCase == null) {
            throw new BaseException(ErrorCodeEnum.NOT_FOUND.getCode(), "评测用例不存在");
        }
        return testCase;
    }

    /**
     * 转换评测集结果，并附带用例数和最近运行摘要。
     */
    private EvaluationDatasetResult toDatasetResult(EvaluationDatasetEntity dataset) {
        long caseCount = caseMapper.selectCount(new LambdaQueryWrapper<EvaluationCaseEntity>()
                .eq(EvaluationCaseEntity::getDatasetId, dataset.getId()));
        EvaluationRunEntity lastRun = runMapper.selectOne(new LambdaQueryWrapper<EvaluationRunEntity>()
                .eq(EvaluationRunEntity::getDatasetId, dataset.getId())
                .orderByDesc(EvaluationRunEntity::getCreateTime)
                .last("limit 1"));
        return new EvaluationDatasetResult(dataset.getId(), dataset.getName(), dataset.getDescription(),
                dataset.getKnowledgeBaseId(), dataset.getTargetType(), caseCount,
                lastRun != null ? lastRun.getId() : null,
                lastRun != null ? lastRun.getStatus() : null,
                lastRun != null ? lastRun.getAvgScore() : null,
                dataset.getCreateTime(), dataset.getUpdateTime());
    }

    /**
     * 转换用例结果。
     */
    private EvaluationCaseResult toCaseResult(EvaluationCaseEntity testCase) {
        return new EvaluationCaseResult(testCase.getId(), testCase.getDatasetId(), testCase.getQuestion(),
                testCase.getExpectedAnswer(), parseStringList(testCase.getExpectedKeywords()),
                parseLongList(testCase.getExpectedChunkIds()), testCase.getExpectedStatus(), testCase.getMetadata(),
                testCase.getEnabled(), testCase.getCreateTime(), testCase.getUpdateTime());
    }

    /**
     * 转换运行汇总结果。
     */
    private EvaluationRunResult toRunResult(EvaluationRunEntity run) {
        return new EvaluationRunResult(run.getId(), run.getDatasetId(), run.getName(), run.getTargetType(),
                run.getStatus(), run.getTotalCount(), run.getPassedCount(), run.getFailedCount(), run.getAvgScore(),
                run.getAvgLatencyMs(), run.getStartedAt(), run.getEndedAt(), run.getErrorMessage(),
                run.getCreateTime(), run.getUpdateTime());
    }

    /**
     * 转换单条运行结果。
     */
    private EvaluationCaseRunResult toCaseRunResult(EvaluationCaseResultRecordEntity result) {
        return new EvaluationCaseRunResult(result.getId(), result.getRunId(), result.getCaseId(),
                result.getQuestion(), result.getExpectedAnswer(), result.getActualAnswer(), result.getRewrittenQuery(),
                result.getExpectedStatus(), result.getActualStatus(), parseStringList(result.getExpectedKeywords()),
                parseLongList(result.getExpectedChunkIds()), parseChunkList(result.getRetrievedChunks()),
                parseChunkList(result.getRerankedChunks()), result.getRetrievalHitScore(), result.getKeywordScore(),
                result.getStatusScore(), result.getTotalScore(), result.getPassed(), parseMap(result.getMetricDetail()),
                result.getLatencyMs(), result.getErrorMessage(), result.getCreateTime());
    }

    /**
     * 转换召回片段展示结果。
     */
    private List<EvaluationChunkResult> toChunkResults(List<SearchResult> chunks) {
        if (chunks == null) {
            return List.of();
        }
        return chunks.stream()
                .map(chunk -> new EvaluationChunkResult(chunk.getChunkId(), chunk.getChunkText(), chunk.getSimilarity(),
                        chunk.getMetadata(), chunk.getVectorScore(), chunk.getBm25Score(), chunk.getRrfScore(), chunk.getRerankScore()))
                .toList();
    }

    /**
     * 解析字符串数组 JSON。
     */
    private List<String> parseStringList(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * 解析 Long 数组 JSON。
     */
    private List<Long> parseLongList(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * 解析召回片段 JSON。
     */
    private List<EvaluationChunkResult> parseChunkList(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * 解析评分明细 JSON。
     */
    private Map<String, Object> parseMap(String json) {
        if (!StringUtils.hasText(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    /**
     * 序列化对象为 JSON。
     */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "[]";
        }
    }

    /**
     * 去除空白，空值转 null。
     */
    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /**
     * 分数保留两位小数。
     */
    private double round(double value) {
        return Math.round(value * 100.0D) / 100.0D;
    }

    /**
     * 单条用例评分明细。
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Score {
        private Double retrievalHitScore;

        private Double keywordScore;

        private Double statusScore;

        private Double totalScore;

        private Boolean passed;

        private Map<String, Object> detail;

    }
}
