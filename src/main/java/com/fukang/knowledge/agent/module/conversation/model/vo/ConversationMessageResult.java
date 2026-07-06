package com.fukang.knowledge.agent.module.conversation.model.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * QA 会话消息。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMessageResult {
    private Long id;

    private Long conversationId;

    private String role;

    private String content;

    private String rewrittenQuery;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

}
