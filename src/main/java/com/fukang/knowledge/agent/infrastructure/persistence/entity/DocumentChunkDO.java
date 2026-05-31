package com.fukang.knowledge.agent.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文档块实体类
 * <p>对应数据库表 document_chunk，存储文档分块后的文本片段。
 * 每条记录代表文档的一个文本块，包含块文本、顺序号和 token 估算数量</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "document_chunk")
@TableName("document_chunk")
public class DocumentChunkDO extends BaseEntity {

    /** 所属文档ID，关联 document 表 */
    @Column(name = "document_id", nullable = false)
    private Long documentId;

    /** 块文本内容，存储分块后的文本片段 */
    @Lob
    @Column(name = "chunk_text", nullable = false, columnDefinition = "TEXT")
    private String chunkText;

    @Lob
    @Column(name = "search_text", columnDefinition = "TEXT")
    private String searchText;

    /** 向量化专用文本，可包含标题和位置信息，不用于前端展示 */
    @Lob
    @Column(name = "embedding_text", columnDefinition = "TEXT")
    private String embeddingText;

    /** embedding_text 的构造策略版本，用于后续重建和审计 */
    @Column(name = "embedding_text_version", length = 32)
    private String embeddingTextVersion;

    /** 文档块所在页码，无法识别时为空 */
    @Column(name = "page_number")
    private Integer pageNumber;

    /** 文档块所属章节标题，无法识别时为空 */
    @Column(name = "section_title", length = 255)
    private String sectionTitle;

    /** 块在文档中的顺序号，从 0 开始递增 */
    @Column(name = "chunk_order", nullable = false)
    private Integer chunkOrder;

    /** 块的 token 估算数量 */
    @Column(name = "token_count")
    private Integer tokenCount;

    @Column(name = "embedding_model_id")
    private Long embeddingModelId;

    @Column(name = "embedding_dimension")
    private Integer embeddingDimension;

    @Column(name = "embedding_version", length = 64)
    private String embeddingVersion;
}
