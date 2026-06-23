package com.fukang.knowledge.agent.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * RAG 评测集。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "evaluation_dataset")
@TableName("evaluation_dataset")
public class EvaluationDatasetDO extends BaseEntity {

    /**
     * 评测集名称。
     */
    @Column(name = "name", nullable = false, length = 120)
    private String name;

    /**
     * 评测集描述。
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 绑定的知识库 ID。
     */
    @Column(name = "knowledge_base_id")
    private Long knowledgeBaseId;

    /**
     * 评测目标类型。
     */
    @Column(name = "target_type", nullable = false, length = 40)
    private String targetType;
}
