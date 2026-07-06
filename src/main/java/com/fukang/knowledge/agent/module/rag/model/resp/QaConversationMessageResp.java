package com.fukang.knowledge.agent.module.rag.model.resp;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * QA 会话消息响应。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QaConversationMessageResp {
    private Long id;

    private Long conversationId;

    private String role;

    private String content;

    private String rewrittenQuery;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

}
