package com.fukang.knowledge.agent.module.evaluation.model.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 评测集保存请求。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationDatasetReq {
    private String name;

    private String description;

    private Long knowledgeBaseId;

}
