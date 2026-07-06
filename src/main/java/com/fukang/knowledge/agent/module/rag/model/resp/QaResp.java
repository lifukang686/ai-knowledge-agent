package com.fukang.knowledge.agent.module.rag.model.resp;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * QA 问答响应 DTO
 *
 * @param answer         回答文本
 * @param rewrittenQuery 改写后的查询文本
 * @param status         处理状态（"success" / "failed"）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QaResp {
    private String answer;

    private String rewrittenQuery;

    private String status;

    private Long conversationId;

}
