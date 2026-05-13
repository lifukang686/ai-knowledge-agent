package com.fukang.knowledge.agent.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文档实体类
 * <p>对应数据库表 document，存储上传文档的基本信息和文件路径，
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
}