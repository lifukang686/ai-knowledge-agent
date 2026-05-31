package com.fukang.knowledge.agent.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * RAG 会话消息实体。
 * <p>保存用户问题、助手回答以及本轮改写查询，供后续多轮问答读取。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "conversation_message")
@TableName("conversation_message")
public class ConversationMessageDO extends BaseEntity {

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    /** 消息角色：user / assistant。 */
    @Column(name = "role", nullable = false, length = 20)
    private String role;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /** 用户问题经上下文改写后的独立查询。 */
    @Column(name = "rewritten_query", columnDefinition = "TEXT")
    private String rewrittenQuery;

    /** 本轮状态：success / no_results / failed。 */
    @Column(name = "status", length = 20)
    private String status;

    /** 预留扩展字段，可保存引用片段、模型、耗时等 JSON。 */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;
}
