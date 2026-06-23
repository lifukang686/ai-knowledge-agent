package com.fukang.knowledge.agent.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * RAG 评测运行记录。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "evaluation_run")
@TableName("evaluation_run")
public class EvaluationRunDO extends BaseEntity {

    @Column(name = "dataset_id", nullable = false)
    private Long datasetId;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Column(name = "target_type", nullable = false, length = 40)
    private String targetType;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "total_count", nullable = false)
    private Integer totalCount;

    @Column(name = "passed_count", nullable = false)
    private Integer passedCount;

    @Column(name = "failed_count", nullable = false)
    private Integer failedCount;

    @Column(name = "avg_score")
    private Double avgScore;

    @Column(name = "avg_latency_ms")
    private Long avgLatencyMs;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Lob
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}
