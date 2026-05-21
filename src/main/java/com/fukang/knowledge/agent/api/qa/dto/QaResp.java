package com.fukang.knowledge.agent.api.qa.dto;

/**
 * QA 问答响应 DTO
 *
 * @param answer         回答文本
 * @param rewrittenQuery 改写后的查询文本
 * @param status         处理状态（"success" / "failed"）
 */
public record QaResp(
        String answer,
        String rewrittenQuery,
        String status
) {}