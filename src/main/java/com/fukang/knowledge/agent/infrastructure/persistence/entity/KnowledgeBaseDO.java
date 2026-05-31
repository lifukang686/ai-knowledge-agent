package com.fukang.knowledge.agent.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识库实体类
 * <p>对应数据库表 knowledge_base，管理不同业务线/项目的文档集合，
 * 每个知识库可包含多份文档，是文档入库和检索的最小隔离单元</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "knowledge_base")
@TableName("knowledge_base")
public class KnowledgeBaseDO extends BaseEntity {

    /** 知识库名称 */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** 知识库描述 */
    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "embedding_model_id")
    private Long embeddingModelId;

    @Column(name = "embedding_dimension")
    private Integer embeddingDimension;

    @Column(name = "embedding_version", length = 64)
    private String embeddingVersion;
}
