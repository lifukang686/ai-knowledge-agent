package com.fukang.knowledge.agent.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * RAG 会话摘要实体。
 * <p>当消息过多时，将早期历史压缩成摘要，避免 prompt 无限增长。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "conversation_summary")
@TableName("conversation_summary")
public class ConversationSummaryDO extends BaseEntity {

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Column(name = "summary", nullable = false, columnDefinition = "TEXT")
    private String summary;

    /** 摘要已经覆盖到的最后一条消息 ID。 */
    @Column(name = "message_until_id")
    private Long messageUntilId;

    /** 粗略 token 估算，便于后续做预算控制。 */
    @Column(name = "token_estimate")
    private Integer tokenEstimate;
}
