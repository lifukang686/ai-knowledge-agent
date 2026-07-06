package com.fukang.knowledge.agent.module.rag.model.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * QA 会话创建请求。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QaConversationCreateReq {
    private Long knowledgeBaseId;

}
