package com.fukang.knowledge.agent.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * RAG 评测用例。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "evaluation_case")
@TableName("evaluation_case")
public class EvaluationCaseDO extends BaseEntity {

    /**
     * 所属评测集 ID。
     */
    @Column(name = "dataset_id", nullable = false)
    private Long datasetId;

    /**
     * 评测问题。
     */
    @Lob
    @Column(name = "question", nullable = false, columnDefinition = "TEXT")
    private String question;

    /**
     * 参考答案。
     */
    @Lob
    @Column(name = "expected_answer", columnDefinition = "TEXT")
    private String expectedAnswer;

    /**
     * 期望关键词 JSON。
     */
    @Lob
    @Column(name = "expected_keywords", columnDefinition = "TEXT")
    private String expectedKeywords;

    /**
     * 期望命中 Chunk ID JSON。
     */
    @Lob
    @Column(name = "expected_chunk_ids", columnDefinition = "TEXT")
    private String expectedChunkIds;

    /**
     * 期望回答状态。
     */
    @Column(name = "expected_status", length = 40)
    private String expectedStatus;

    /**
     * 扩展元数据。
     */
    @Lob
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    /**
     * 是否启用该用例。
     */
    @Column(name = "enabled", nullable = false)
    private Boolean enabled;
}
