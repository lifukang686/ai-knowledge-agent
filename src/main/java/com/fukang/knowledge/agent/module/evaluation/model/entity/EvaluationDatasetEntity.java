package com.fukang.knowledge.agent.module.evaluation.model.entity;

import com.fukang.knowledge.agent.infrastructure.model.BaseEntity;

import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * RAG 璇勬祴闆嗐€? */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "evaluation_dataset")
@TableName("evaluation_dataset")
public class EvaluationDatasetEntity extends BaseEntity {

    /**
     * 璇勬祴闆嗗悕绉般€?     */
    @Column(name = "name", nullable = false, length = 120)
    private String name;

    /**
     * 璇勬祴闆嗘弿杩般€?     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 缁戝畾鐨勭煡璇嗗簱 ID銆?     */
    @Column(name = "knowledge_base_id")
    private Long knowledgeBaseId;

    /**
     * 璇勬祴鐩爣绫诲瀷銆?     */
    @Column(name = "target_type", nullable = false, length = 40)
    private String targetType;
}
