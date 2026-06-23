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

    /**
     * 所属运行 ID。
     */
    @Column(name = "run_id", nullable = false)
    private Long runId;

    /**
     * 原始用例 ID。
     */
    @Column(name = "case_id", nullable = false)
    private Long caseId;

    /**
     * 问题快照。
     */
    @Lob
    @Column(name = "question", nullable = false, columnDefinition = "TEXT")
    private String question;

    /**
     * 参考答案快照。
     */
    @Lob
    @Column(name = "expected_answer", columnDefinition = "TEXT")
    private String expectedAnswer;

    /**
     * 实际回答。
     */
    @Lob
    @Column(name = "actual_answer", columnDefinition = "TEXT")
    private String actualAnswer;

    /**
     * 改写后的查询。
     */
    @Lob
    @Column(name = "rewritten_query", columnDefinition = "TEXT")
    private String rewrittenQuery;

    /**
     * 期望状态。
     */
    @Column(name = "expected_status", length = 40)
    private String expectedStatus;

    /**
     * 实际状态。
     */
    @Column(name = "actual_status", length = 40)
    private String actualStatus;

    /**
     * 期望关键词 JSON。
     */
    @Lob
    @Column(name = "expected_keywords", columnDefinition = "TEXT")
    private String expectedKeywords;

    /**
     * 期望 Chunk ID JSON。
     */
    @Lob
    @Column(name = "expected_chunk_ids", columnDefinition = "TEXT")
    private String expectedChunkIds;

    /**
     * 原始召回片段 JSON。
     */
    @Lob
    @Column(name = "retrieved_chunks", columnDefinition = "TEXT")
    private String retrievedChunks;

    /**
     * 重排后片段 JSON。
     */
    @Lob
    @Column(name = "reranked_chunks", columnDefinition = "TEXT")
    private String rerankedChunks;

    /**
     * 召回命中得分。
     */
    @Column(name = "retrieval_hit_score")
    private Double retrievalHitScore;

    /**
     * 关键词覆盖得分。
     */
    @Column(name = "keyword_score")
    private Double keywordScore;

    /**
     * 状态匹配得分。
     */
    @Column(name = "status_score")
    private Double statusScore;

    /**
     * 总分。
     */
    @Column(name = "total_score", nullable = false)
    private Double totalScore;

    /**
     * 是否通过。
     */
    @Column(name = "passed", nullable = false)
    private Boolean passed;

    /**
     * 评分明细 JSON。
     */
    @Lob
    @Column(name = "metric_detail", columnDefinition = "TEXT")
    private String metricDetail;

    /**
     * 执行耗时。
     */
    @Column(name = "latency_ms")
    private Long latencyMs;

    /**
     * 单条用例错误信息。
     */
    @Lob
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}
