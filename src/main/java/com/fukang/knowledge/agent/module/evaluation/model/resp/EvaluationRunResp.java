package com.fukang.knowledge.agent.module.evaluation.model.resp;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 评测运行响应。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationRunResp {
    private Long id;

    private Long datasetId;

    private String name;

    private String targetType;

    private String status;

    private Integer totalCount;

    private Integer passedCount;

    private Integer failedCount;

    private Double avgScore;

    private Long avgLatencyMs;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    private String errorMessage;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

}
