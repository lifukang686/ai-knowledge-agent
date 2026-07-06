package com.fukang.knowledge.agent.module.rag.model.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * QA 问答请求 DTO
 *
 * @param question        用户自然语言问题
 * @param knowledgeBaseId 目标知识库ID
 * @param conversationId  会话ID（可选，用于多轮对话上下文关联）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QaReq {
    private String question;

    private Long knowledgeBaseId;

    private Long conversationId;

}