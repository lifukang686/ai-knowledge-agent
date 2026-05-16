package com.fukang.knowledge.agent.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 嵌入向量索引实体类
 * <p>对应数据库表 embedding_index，存储文档块对应的向量嵌入数据。
 * 向量字段使用 pgvector 类型存储，用于后续语义检索。
 * MVP 阶段向量嵌入由 Week 3 RAG 模块实现后填充</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "embedding_index")
@TableName("embedding_index")
public class EmbeddingIndexDO extends BaseEntity {

    /** 关联的文档块ID，对应 document_chunk 表主键 */
    @Column(name = "chunk_id", nullable = false)
    private Long chunkId;

    /** 所属知识库ID，关联 knowledge_base 表 */
    @Column(name = "knowledge_base_id", nullable = false)
    private Long knowledgeBaseId;

    /** 向量数据，pgvector 类型，后续由嵌入模型计算后填充 */
    @Lob
    @Column(name = "vector", columnDefinition = "TEXT")
    private String vector;

    /** 向量元数据（JSON格式），可存储原文摘要、模型版本等信息 */
    @Lob
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;
}