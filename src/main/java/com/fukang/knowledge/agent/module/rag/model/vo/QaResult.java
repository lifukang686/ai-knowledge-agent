package com.fukang.knowledge.agent.module.rag.model.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RAG 问答结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QaResult {
    private String answer;

    private String rewrittenQuery;

    private String status;

    private Long conversationId;

}
