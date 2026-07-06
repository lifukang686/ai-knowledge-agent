package com.fukang.knowledge.agent.module.evaluation.model.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 评测用例结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationCaseResult {
    private Long id;

    private Long datasetId;

    private String question;

    private String expectedAnswer;

    private List<String> expectedKeywords;

    private List<Long> expectedChunkIds;

    private String expectedStatus;

    private String metadata;

    private Boolean enabled;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

}
