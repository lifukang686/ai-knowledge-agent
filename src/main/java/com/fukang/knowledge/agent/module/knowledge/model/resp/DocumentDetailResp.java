package com.fukang.knowledge.agent.module.knowledge.model.resp;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 文档详情响应 DTO
 * <p>返回给前端的文档完整信息，包含元数据和解析后的文本内容。
 * 用于文档详情浏览页面展示文档标题、内容、创建时间、更新时间等完整信息。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDetailResp {
    /** 文档ID（Long→String 序列化） */
    private Long id;

    /** 文档标题（原始文件名） */
    private String title;

    /** 文档解析文本，由已入库的 chunk 拼接得到 */
    private String content;

    /** 文件在 MinIO 中的存储路径 */
    private String filePath;

    /** 所属知识库ID */
    private Long knowledgeBaseId;

    /** 处理状态 */
    private String status;

    /** 上传者 */
    private String uploadedBy;

    /** 分块数量 */
    private long chunkCount;

    /** 文件大小（字节） */
    private long fileSize;

    /** 文档向量化实际使用的模型配置 ID */
    private Long embeddingModelId;

    /** 文档向量维度 */
    private Integer embeddingDimension;

    /** 文档向量化模型版本标识 */
    private String embeddingVersion;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

}
