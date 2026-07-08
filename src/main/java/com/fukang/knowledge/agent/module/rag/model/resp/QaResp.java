package com.fukang.knowledge.agent.module.rag.model.resp;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * QA 问答响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QaResp {
    /** 回答文本 */
    private String answer;

    /** 改写后的查询文本 */
    private String rewrittenQuery;

    /** 处理状态（"success" / "failed"） */
    private String status;

    private Long conversationId;

}
