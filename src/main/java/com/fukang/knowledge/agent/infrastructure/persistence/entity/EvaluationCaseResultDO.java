package com.fukang.knowledge.agent.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * RAG 评测单条用例结果。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "evaluation_case_result")
@TableName("evaluation_case_result")
public class EvaluationCaseResultDO extends BaseEntity {

    @Column(name = "run_id", nullable = false)
    private Long runId;

    @Column(name = "case_id", nullable = false)
    private Long caseId;

    @Lob
    @Column(name = "question", nullable = false, columnDefinition = "TEXT")
    private String question;

    @Lob
    @Column(name = "expected_answer", columnDefinition = "TEXT")
    private String expectedAnswer;

    @Lob
    @Column(name = "actual_answer", columnDefinition = "TEXT")
    private String actualAnswer;

    @Lob
    @Column(name = "rewritten_query", columnDefinition = "TEXT")
    private String rewrittenQuery;

    @Column(name = "expected_status", length = 40)
    private String expectedStatus;

    @Column(name = "actual_status", length = 40)
    private String actualStatus;

    @Lob
    @Column(name = "expected_keywords", columnDefinition = "TEXT")
    private String expectedKeywords;

    @Lob
    @Column(name = "expected_chunk_ids", columnDefinition = "TEXT")
    private String expectedChunkIds;

    @Lob
    @Column(name = "retrieved_chunks", columnDefinition = "TEXT")
    private String retrievedChunks;

    @Lob
    @Column(name = "reranked_chunks", columnDefinition = "TEXT")
    private String rerankedChunks;

    @Column(name = "retrieval_hit_score")
    private Double retrievalHitScore;

    @Column(name = "keyword_score")
    private Double keywordScore;

    @Column(name = "status_score")
    private Double statusScore;

    @Column(name = "total_score", nullable = false)
    private Double totalScore;

    @Column(name = "passed", nullable = false)
    private Boolean passed;

    @Lob
    @Column(name = "metric_detail", columnDefinition = "TEXT")
    private String metricDetail;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Lob
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}
