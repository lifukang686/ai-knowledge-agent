package com.fukang.knowledge.agent.module.evaluation.model.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 评测集结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationDatasetResult {
    private Long id;

    private String name;

    private String description;

    private Long knowledgeBaseId;

    private String targetType;

    private long caseCount;

    private Long lastRunId;

    private String lastRunStatus;

    private Double lastAvgScore;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

}
