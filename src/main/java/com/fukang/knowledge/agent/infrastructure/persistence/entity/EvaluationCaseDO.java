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

    @Column(name = "dataset_id", nullable = false)
    private Long datasetId;

    @Lob
    @Column(name = "question", nullable = false, columnDefinition = "TEXT")
    private String question;

    @Lob
    @Column(name = "expected_answer", columnDefinition = "TEXT")
    private String expectedAnswer;

    @Lob
    @Column(name = "expected_keywords", columnDefinition = "TEXT")
    private String expectedKeywords;

    @Lob
    @Column(name = "expected_chunk_ids", columnDefinition = "TEXT")
    private String expectedChunkIds;

    @Column(name = "expected_status", length = 40)
    private String expectedStatus;

    @Lob
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled;
}
