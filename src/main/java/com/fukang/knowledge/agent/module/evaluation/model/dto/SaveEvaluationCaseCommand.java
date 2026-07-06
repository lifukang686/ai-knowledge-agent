package com.fukang.knowledge.agent.module.evaluation.model.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * 保存评测用例命令。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaveEvaluationCaseCommand {
    private String question;

    private String expectedAnswer;

    private List<String> expectedKeywords;

    private List<Long> expectedChunkIds;

    private String expectedStatus;

    private String metadata;

    private Boolean enabled;

}
