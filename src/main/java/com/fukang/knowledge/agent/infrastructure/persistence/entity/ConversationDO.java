package com.fukang.knowledge.agent.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * RAG 会话主表实体。
 * <p>一条会话聚合多轮问答消息，用于恢复短期记忆和摘要记忆。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "conversation")
@TableName("conversation")
public class ConversationDO extends BaseEntity {

    /** 会话所属用户，当前认证未完善时可能使用默认用户。 */
    @Column(name = "user_id")
    private Long userId;

    /** 会话关联的知识库；为空表示跨知识库问答。 */
    @Column(name = "knowledge_base_id")
    private Long knowledgeBaseId;

    /** 会话标题，默认取首轮问题的前一段文本。 */
    @Column(name = "title", length = 200)
    private String title;

    /** 会话状态：active / archived。 */
    @Column(name = "status", length = 20)
    private String status;
}
