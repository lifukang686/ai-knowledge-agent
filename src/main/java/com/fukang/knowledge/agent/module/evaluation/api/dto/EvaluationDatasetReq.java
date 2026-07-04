package com.fukang.knowledge.agent.module.evaluation.api.dto;

/**
 * 评测集保存请求。
 */
public record EvaluationDatasetReq(
        String name,
        String description,
        Long knowledgeBaseId
) {
}
