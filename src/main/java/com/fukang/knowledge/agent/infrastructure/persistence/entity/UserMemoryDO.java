package com.fukang.knowledge.agent.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户级记忆实体，跨会话保存稳定偏好和事实。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "user_memory")
@TableName("user_memory")
public class UserMemoryDO extends BaseEntity {

    /** 记忆所属用户。 */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 记忆类型：profile / preference / fact / goal。 */
    @Column(name = "memory_type", nullable = false, length = 30)
    private String memoryType;

    /** 记忆内容，保持短句。 */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /** 可信度，默认由模型抽取给出。 */
    @Column(name = "confidence", nullable = false)
    private Double confidence;

    /** 来源会话。 */
    @Column(name = "source_conversation_id")
    private Long sourceConversationId;

    /** 来源消息。 */
    @Column(name = "source_message_id")
    private Long sourceMessageId;

    /** 记忆状态：active / archived。 */
    @Column(name = "status", nullable = false, length = 20)
    private String status;
}
