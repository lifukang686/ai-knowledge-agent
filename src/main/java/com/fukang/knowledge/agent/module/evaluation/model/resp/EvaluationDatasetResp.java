package com.fukang.knowledge.agent.module.evaluation.model.resp;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 评测集响应。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationDatasetResp {
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
