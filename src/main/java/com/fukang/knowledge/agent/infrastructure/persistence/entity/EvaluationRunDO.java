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

    /**
     * 所属评测集 ID。
     */
    @Column(name = "dataset_id", nullable = false)
    private Long datasetId;

    /**
     * 运行名称。
     */
    @Column(name = "name", nullable = false, length = 160)
    private String name;

    /**
     * 评测目标类型。
     */
    @Column(name = "target_type", nullable = false, length = 40)
    private String targetType;

    /**
     * 运行状态。
     */
    @Column(name = "status", nullable = false, length = 30)
    private String status;

    /**
     * 用例总数。
     */
    @Column(name = "total_count", nullable = false)
    private Integer totalCount;

    /**
     * 通过用例数。
     */
    @Column(name = "passed_count", nullable = false)
    private Integer passedCount;

    /**
     * 失败用例数。
     */
    @Column(name = "failed_count", nullable = false)
    private Integer failedCount;

    /**
     * 平均得分。
     */
    @Column(name = "avg_score")
    private Double avgScore;

    /**
     * 平均耗时。
     */
    @Column(name = "avg_latency_ms")
    private Long avgLatencyMs;

    /**
     * 开始时间。
     */
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    /**
     * 结束时间。
     */
    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    /**
     * 失败信息。
     */
    @Lob
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}
