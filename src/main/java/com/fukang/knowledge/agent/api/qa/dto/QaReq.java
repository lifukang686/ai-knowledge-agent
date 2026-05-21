package com.fukang.knowledge.agent.api.qa.dto;

/**
 * QA 问答请求 DTO
 *
 * @param question        用户自然语言问题
 * @param knowledgeBaseId 目标知识库ID
 * @param conversationId  会话ID（可选，用于多轮对话上下文关联）
 */
public record QaReq(
        String question,
        Long knowledgeBaseId,
        Long conversationId
) {}