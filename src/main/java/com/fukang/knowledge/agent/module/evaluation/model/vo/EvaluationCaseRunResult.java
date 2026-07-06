package com.fukang.knowledge.agent.module.evaluation.model.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 单条评测运行结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationCaseRunResult {
    private Long id;

    private Long runId;

    private Long caseId;

    private String question;

    private String expectedAnswer;

    private String actualAnswer;

    private String rewrittenQuery;

    private String expectedStatus;

    private String actualStatus;

    private List<String> expectedKeywords;

    private List<Long> expectedChunkIds;

    private List<EvaluationChunkResult> retrievedChunks;

    private List<EvaluationChunkResult> rerankedChunks;

    private Double retrievalHitScore;

    private Double keywordScore;

    private Double statusScore;

    private Double totalScore;

    private Boolean passed;

    private Map<String, Object> metricDetail;

    private Long latencyMs;

    private String errorMessage;

    private LocalDateTime createTime;

}
