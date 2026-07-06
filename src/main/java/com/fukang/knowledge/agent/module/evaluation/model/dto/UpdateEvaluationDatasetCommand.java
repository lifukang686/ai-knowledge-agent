package com.fukang.knowledge.agent.module.evaluation.model.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新评测集命令。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEvaluationDatasetCommand {
    private String name;

    private String description;

    private Long knowledgeBaseId;

}
