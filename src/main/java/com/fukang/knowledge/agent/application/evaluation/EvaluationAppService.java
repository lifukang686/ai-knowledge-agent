package com.fukang.knowledge.agent.application.evaluation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fukang.knowledge.agent.application.evaluation.command.CreateEvaluationDatasetCommand;
import com.fukang.knowledge.agent.application.evaluation.command.SaveEvaluationCaseCommand;
import com.fukang.knowledge.agent.application.evaluation.command.UpdateEvaluationDatasetCommand;
import com.fukang.knowledge.agent.application.evaluation.result.EvaluationCaseResult;
import com.fukang.knowledge.agent.application.evaluation.result.EvaluationCaseRunResult;
import com.fukang.knowledge.agent.application.evaluation.result.EvaluationChunkResult;
import com.fukang.knowledge.agent.application.evaluation.result.EvaluationDatasetResult;
import com.fukang.knowledge.agent.application.evaluation.result.EvaluationRunResult;
import com.fukang.knowledge.agent.application.knowledge.port.KnowledgeBaseRepository;
import com.fukang.knowledge.agent.application.rag.RagAppService;
import com.fukang.knowledge.agent.application.rag.result.RagEvalResult;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.common.result.PageResponse;
import com.fukang.knowledge.agent.domain.rag.model.SearchResult;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.EvaluationCaseDO;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.EvaluationCaseResultDO;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.EvaluationDatasetDO;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.EvaluationRunDO;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.EvaluationCaseMapper;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.EvaluationCaseResultMapper;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.EvaluationDatasetMapper;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.EvaluationRunMapper;
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
public class EvaluationAppService {

    private static final String TARGET_TYPE_RAG_QA = "RAG_QA";
    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_FAILED = "FAILED";
    private static final double PASS_THRESHOLD = 70.0D;

    private final EvaluationDatasetMapper datasetMapper;
    private final EvaluationCaseMapper caseMapper;
    private final EvaluationRunMapper runMapper;
    private final EvaluationCaseResultMapper caseResultMapper;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final RagAppService ragAppService;
    private final ObjectMapper objectMapper;

    public PageResponse<EvaluationDatasetResult> listDatasets(long page, long pageSize, String keyword,
                                                              Long knowledgeBaseId) {
        LambdaQueryWrapper<EvaluationDatasetDO> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(EvaluationDatasetDO::getName, keyword)
                    .or()
                    .like(EvaluationDatasetDO::getDescription, keyword));
        }
        if (knowledgeBaseId != null) {
            wrapper.eq(EvaluationDatasetDO::getKnowledgeBaseId, knowledgeBaseId);
        }
        wrapper.orderByDesc(EvaluationDatasetDO::getCreateTime);
        IPage<EvaluationDatasetDO> resultPage = datasetMapper.selectPage(new Page<>(page, pageSize), wrapper);
        List<EvaluationDatasetResult> items = resultPage.getRecords().stream()
                .map(this::toDatasetResult)
                .toList();
        return new PageResponse<>(items, resultPage.getTotal(), resultPage.getCurrent(), resultPage.getSize());
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createDataset(CreateEvaluationDatasetCommand command) {
        validateDataset(command.name(), command.knowledgeBaseId());
        EvaluationDatasetDO dataset = new EvaluationDatasetDO();
        dataset.setName(command.name().trim());
        dataset.setDescription(trimToNull(command.description()));
        dataset.setKnowledgeBaseId(command.knowledgeBaseId());
        dataset.setTargetType(TARGET_TYPE_RAG_QA);
        datasetMapper.insert(dataset);
        return dataset.getId();
    }

    public EvaluationDatasetResult getDataset(Long id) {
        return toDatasetResult(findDataset(id));
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateDataset(Long id, UpdateEvaluationDatasetCommand command) {
        EvaluationDatasetDO dataset = findDataset(id);
        validateDataset(command.name(), command.knowledgeBaseId());
        dataset.setName(command.name().trim());
        dataset.setDescription(trimToNull(command.description()));
        dataset.setKnowledgeBaseId(command.knowledgeBaseId());
        datasetMapper.updateById(dataset);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteDataset(Long id) {
        findDataset(id);
        List<Long> runIds = runMapper.selectList(new LambdaQueryWrapper<EvaluationRunDO>()
                        .select(EvaluationRunDO::getId)
                        .eq(EvaluationRunDO::getDatasetId, id))
                .stream()
                .map(EvaluationRunDO::getId)
                .toList();
        if (!runIds.isEmpty()) {
            caseResultMapper.delete(new LambdaQueryWrapper<EvaluationCaseResultDO>()
                    .in(EvaluationCaseResultDO::getRunId, runIds));
        }
        runMapper.delete(new LambdaQueryWrapper<EvaluationRunDO>().eq(EvaluationRunDO::getDatasetId, id));
        caseMapper.delete(new LambdaQueryWrapper<EvaluationCaseDO>().eq(EvaluationCaseDO::getDatasetId, id));
        datasetMapper.deleteById(id);
    }

    public PageResponse<EvaluationCaseResult> listCases(Long datasetId, long page, long pageSize) {
        findDataset(datasetId);
        IPage<EvaluationCaseDO> resultPage = caseMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<EvaluationCaseDO>()
                        .eq(EvaluationCaseDO::getDatasetId, datasetId)
                        .orderByDesc(EvaluationCaseDO::getCreateTime));
        return new PageResponse<>(resultPage.getRecords().stream().map(this::toCaseResult).toList(),
                resultPage.getTotal(), resultPage.getCurrent(), resultPage.getSize());
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createCase(Long datasetId, SaveEvaluationCaseCommand command) {
        findDataset(datasetId);
        validateCase(command);
        EvaluationCaseDO testCase = new EvaluationCaseDO();
        fillCase(testCase, datasetId, command);
        caseMapper.insert(testCase);
        return testCase.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateCase(Long id, SaveEvaluationCaseCommand command) {
        EvaluationCaseDO testCase = findCase(id);
        validateCase(command);
        fillCase(testCase, testCase.getDatasetId(), command);
        caseMapper.updateById(testCase);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteCase(Long id) {
        findCase(id);
        caseResultMapper.delete(new LambdaQueryWrapper<EvaluationCaseResultDO>()
                .eq(EvaluationCaseResultDO::getCaseId, id));
        caseMapper.deleteById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long runDataset(Long datasetId) {
        EvaluationDatasetDO dataset = findDataset(datasetId);
        EvaluationRunDO run = createRun(dataset);
        List<EvaluationCaseDO> cases = caseMapper.selectList(new LambdaQueryWrapper<EvaluationCaseDO>()
                .eq(EvaluationCaseDO::getDatasetId, datasetId)
                .eq(EvaluationCaseDO::getEnabled, true)
                .orderByAsc(EvaluationCaseDO::getCreateTime));

        if (cases.isEmpty()) {
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
        for (EvaluationCaseDO testCase : cases) {
            EvaluationCaseResultDO result = runCase(run.getId(), dataset.getKnowledgeBaseId(), testCase);
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

    public EvaluationRunResult getRun(Long runId) {
        EvaluationRunDO run = runMapper.selectById(runId);
        if (run == null) {
            throw new BaseException(ErrorCodeEnum.NOT_FOUND.getCode(), "评测运行记录不存在");
        }
        return toRunResult(run);
    }

    public PageResponse<EvaluationCaseRunResult> listRunResults(Long runId, long page, long pageSize) {
        getRun(runId);
        IPage<EvaluationCaseResultDO> resultPage = caseResultMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<EvaluationCaseResultDO>()
                        .eq(EvaluationCaseResultDO::getRunId, runId)
                        .orderByAsc(EvaluationCaseResultDO::getCreateTime));
        return new PageResponse<>(resultPage.getRecords().stream().map(this::toCaseRunResult).toList(),
                resultPage.getTotal(), resultPage.getCurrent(), resultPage.getSize());
    }

    private EvaluationRunDO createRun(EvaluationDatasetDO dataset) {
        EvaluationRunDO run = new EvaluationRunDO();
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

    private EvaluationCaseResultDO runCase(Long runId, Long knowledgeBaseId, EvaluationCaseDO testCase) {
        EvaluationCaseResultDO result = baseCaseResult(runId, testCase);
        try {
            RagEvalResult ragResult = ragAppService.evaluate(testCase.getQuestion(), knowledgeBaseId);
            Score score = score(testCase, ragResult);
            result.setActualAnswer(ragResult.answer());
            result.setRewrittenQuery(ragResult.rewrittenQuery());
            result.setActualStatus(ragResult.status());
            result.setRetrievedChunks(toJson(toChunkResults(ragResult.retrievedChunks())));
            result.setRerankedChunks(toJson(toChunkResults(ragResult.rerankedChunks())));
            result.setRetrievalHitScore(score.retrievalHitScore());
            result.setKeywordScore(score.keywordScore());
            result.setStatusScore(score.statusScore());
            result.setTotalScore(score.totalScore());
            result.setPassed(score.passed());
            result.setMetricDetail(toJson(score.detail()));
            result.setLatencyMs(ragResult.latencyMs());
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

    private EvaluationCaseResultDO baseCaseResult(Long runId, EvaluationCaseDO testCase) {
        EvaluationCaseResultDO result = new EvaluationCaseResultDO();
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

    private Score score(EvaluationCaseDO testCase, RagEvalResult ragResult) {
        List<Double> parts = new ArrayList<>();
        Map<String, Object> detail = new LinkedHashMap<>();

        List<Long> expectedChunkIds = parseLongList(testCase.getExpectedChunkIds());
        Double retrievalScore = null;
        if (!expectedChunkIds.isEmpty()) {
            boolean hit = hasChunkHit(expectedChunkIds, ragResult.retrievedChunks())
                    || hasChunkHit(expectedChunkIds, ragResult.rerankedChunks());
            retrievalScore = hit ? 100.0D : 0.0D;
            parts.add(retrievalScore);
            detail.put("retrievalHit", hit);
        }

        List<String> keywords = parseStringList(testCase.getExpectedKeywords());
        Double keywordScore = null;
        if (!keywords.isEmpty()) {
            long hitCount = keywords.stream().filter(keyword -> contains(ragResult.answer(), keyword)).count();
            keywordScore = round(hitCount * 100.0D / keywords.size());
            parts.add(keywordScore);
            detail.put("keywordHitCount", hitCount);
            detail.put("keywordTotal", keywords.size());
        }

        Double statusScore = null;
        if (StringUtils.hasText(testCase.getExpectedStatus())) {
            boolean match = testCase.getExpectedStatus().equalsIgnoreCase(
                    ragResult.status() != null ? ragResult.status() : "");
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

    private boolean hasChunkHit(List<Long> expectedChunkIds, List<SearchResult> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return false;
        }
        return chunks.stream().map(SearchResult::chunkId).filter(Objects::nonNull).anyMatch(expectedChunkIds::contains);
    }

    private boolean contains(String answer, String keyword) {
        if (!StringUtils.hasText(answer) || !StringUtils.hasText(keyword)) {
            return false;
        }
        return answer.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    private void validateDataset(String name, Long knowledgeBaseId) {
        if (!StringUtils.hasText(name)) {
            throw new BaseException(ErrorCodeEnum.BAD_REQUEST.getCode(), "评测集名称不能为空");
        }
        if (knowledgeBaseId != null && knowledgeBaseRepository.findById(knowledgeBaseId) == null) {
            throw new BaseException(ErrorCodeEnum.KNOWLEDGE_BASE_NOT_EXIST);
        }
    }

    private void validateCase(SaveEvaluationCaseCommand command) {
        if (command == null || !StringUtils.hasText(command.question())) {
            throw new BaseException(ErrorCodeEnum.BAD_REQUEST.getCode(), "评测问题不能为空");
        }
    }

    private void fillCase(EvaluationCaseDO testCase, Long datasetId, SaveEvaluationCaseCommand command) {
        testCase.setDatasetId(datasetId);
        testCase.setQuestion(command.question().trim());
        testCase.setExpectedAnswer(trimToNull(command.expectedAnswer()));
        testCase.setExpectedKeywords(toJson(command.expectedKeywords() != null ? command.expectedKeywords() : List.of()));
        testCase.setExpectedChunkIds(toJson(command.expectedChunkIds() != null ? command.expectedChunkIds() : List.of()));
        testCase.setExpectedStatus(trimToNull(command.expectedStatus()));
        testCase.setMetadata(trimToNull(command.metadata()));
        testCase.setEnabled(command.enabled() == null || command.enabled());
    }

    private EvaluationDatasetDO findDataset(Long id) {
        EvaluationDatasetDO dataset = datasetMapper.selectById(id);
        if (dataset == null) {
            throw new BaseException(ErrorCodeEnum.NOT_FOUND.getCode(), "评测集不存在");
        }
        return dataset;
    }

    private EvaluationCaseDO findCase(Long id) {
        EvaluationCaseDO testCase = caseMapper.selectById(id);
        if (testCase == null) {
            throw new BaseException(ErrorCodeEnum.NOT_FOUND.getCode(), "评测用例不存在");
        }
        return testCase;
    }

    private EvaluationDatasetResult toDatasetResult(EvaluationDatasetDO dataset) {
        long caseCount = caseMapper.selectCount(new LambdaQueryWrapper<EvaluationCaseDO>()
                .eq(EvaluationCaseDO::getDatasetId, dataset.getId()));
        EvaluationRunDO lastRun = runMapper.selectOne(new LambdaQueryWrapper<EvaluationRunDO>()
                .eq(EvaluationRunDO::getDatasetId, dataset.getId())
                .orderByDesc(EvaluationRunDO::getCreateTime)
                .last("limit 1"));
        return new EvaluationDatasetResult(dataset.getId(), dataset.getName(), dataset.getDescription(),
                dataset.getKnowledgeBaseId(), dataset.getTargetType(), caseCount,
                lastRun != null ? lastRun.getId() : null,
                lastRun != null ? lastRun.getStatus() : null,
                lastRun != null ? lastRun.getAvgScore() : null,
                dataset.getCreateTime(), dataset.getUpdateTime());
    }

    private EvaluationCaseResult toCaseResult(EvaluationCaseDO testCase) {
        return new EvaluationCaseResult(testCase.getId(), testCase.getDatasetId(), testCase.getQuestion(),
                testCase.getExpectedAnswer(), parseStringList(testCase.getExpectedKeywords()),
                parseLongList(testCase.getExpectedChunkIds()), testCase.getExpectedStatus(), testCase.getMetadata(),
                testCase.getEnabled(), testCase.getCreateTime(), testCase.getUpdateTime());
    }

    private EvaluationRunResult toRunResult(EvaluationRunDO run) {
        return new EvaluationRunResult(run.getId(), run.getDatasetId(), run.getName(), run.getTargetType(),
                run.getStatus(), run.getTotalCount(), run.getPassedCount(), run.getFailedCount(), run.getAvgScore(),
                run.getAvgLatencyMs(), run.getStartedAt(), run.getEndedAt(), run.getErrorMessage(),
                run.getCreateTime(), run.getUpdateTime());
    }

    private EvaluationCaseRunResult toCaseRunResult(EvaluationCaseResultDO result) {
        return new EvaluationCaseRunResult(result.getId(), result.getRunId(), result.getCaseId(),
                result.getQuestion(), result.getExpectedAnswer(), result.getActualAnswer(), result.getRewrittenQuery(),
                result.getExpectedStatus(), result.getActualStatus(), parseStringList(result.getExpectedKeywords()),
                parseLongList(result.getExpectedChunkIds()), parseChunkList(result.getRetrievedChunks()),
                parseChunkList(result.getRerankedChunks()), result.getRetrievalHitScore(), result.getKeywordScore(),
                result.getStatusScore(), result.getTotalScore(), result.getPassed(), parseMap(result.getMetricDetail()),
                result.getLatencyMs(), result.getErrorMessage(), result.getCreateTime());
    }

    private List<EvaluationChunkResult> toChunkResults(List<SearchResult> chunks) {
        if (chunks == null) {
            return List.of();
        }
        return chunks.stream()
                .map(chunk -> new EvaluationChunkResult(chunk.chunkId(), chunk.chunkText(), chunk.similarity(),
                        chunk.metadata(), chunk.vectorScore(), chunk.bm25Score(), chunk.rrfScore(), chunk.rerankScore()))
                .toList();
    }

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

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "[]";
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private double round(double value) {
        return Math.round(value * 100.0D) / 100.0D;
    }

    private record Score(
            Double retrievalHitScore,
            Double keywordScore,
            Double statusScore,
            Double totalScore,
            Boolean passed,
            Map<String, Object> detail
    ) {
    }
}
