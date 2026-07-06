package com.fukang.knowledge.agent.module.evaluation.model.resp;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 评测单条运行结果响应。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationCaseResultResp {
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

    private List<EvaluationChunkResp> retrievedChunks;

    private List<EvaluationChunkResp> rerankedChunks;

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
