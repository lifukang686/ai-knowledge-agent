package com.fukang.knowledge.agent.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文档实体类
 * <p>对应数据库表 document，存储上传文档的基本信息、文件路径和处理状态，
 * 每条记录代表一份已上传到 MinIO 的原始文档</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "document")
@TableName("document")
public class DocumentDO extends BaseEntity {

    /** 所属知识库ID，关联 knowledge_base 表 */
    @Column(name = "knowledge_base_id", nullable = false)
    private Long knowledgeBaseId;

    /** 文档标题，取上传时的原始文件名 */
    @Column(name = "title", nullable = false, length = 255)
    private String title;

    /** 文档在 MinIO 中的存储路径 */
    @Column(name = "file_path", length = 500)
    private String filePath;

    /** 上传者用户ID，关联 sys_user 表 */
    @Column(name = "uploader_id")
    private Long uploaderId;

    /** 文档处理状态：pending / parsing / chunking / embedding / completed / failed */
    @Column(name = "status", length = 20)
    private String status;

    /** 处理错误信息，仅在 status=failed 时有值 */
    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    /** 处理的文档块总数，处理完成后填充 */
    @Column(name = "chunk_count")
    private Integer chunkCount;

    /** 处理耗时（毫秒），处理完成后填充 */
    @Column(name = "processing_duration_ms")
    private Long processingDurationMs;

    @Column(name = "embedding_model_id")
    private Long embeddingModelId;

    @Column(name = "embedding_dimension")
    private Integer embeddingDimension;

    @Column(name = "embedding_version", length = 64)
    private String embeddingVersion;
}
