package com.fukang.knowledge.agent.api.evaluation.dto;

/**
 * 评测集保存请求。
 */
public record EvaluationDatasetReq(
        String name,
        String description,
        Long knowledgeBaseId
) {
}
