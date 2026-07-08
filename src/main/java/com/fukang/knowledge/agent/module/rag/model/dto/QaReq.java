package com.fukang.knowledge.agent.module.rag.model.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * QA 问答请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QaReq {
    /** 用户自然语言问题 */
    private String question;

    /** 目标知识库ID */
    private Long knowledgeBaseId;

    /** 会话ID（可选，用于多轮对话上下文关联） */
    private Long conversationId;

}