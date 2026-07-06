package com.fukang.knowledge.agent.module.evaluation.model.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * 评测用例保存请求。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationCaseReq {
    private String question;

    private String expectedAnswer;

    private List<String> expectedKeywords;

    private List<Long> expectedChunkIds;

    private String expectedStatus;

    private String metadata;

    private Boolean enabled;

}
