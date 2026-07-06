package com.fukang.knowledge.agent.module.conversation.model.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * QA 会话列表项。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationListItemResult {
    private Long id;

    private Long knowledgeBaseId;

    private String title;

    private String status;

    private long messageCount;

    private LocalDateTime lastMessageAt;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

}
