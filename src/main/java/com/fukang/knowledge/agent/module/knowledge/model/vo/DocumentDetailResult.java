package com.fukang.knowledge.agent.module.knowledge.model.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 文档详情查询结果，content 为解析后的 chunk 文本拼接。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDetailResult {
    private Long id;

    private String title;

    private String content;

    private String filePath;

    private Long knowledgeBaseId;

    private String status;

    private String uploadedBy;

    private long chunkCount;

    private long fileSize;

    private Long embeddingModelId;

    private Integer embeddingDimension;

    private String embeddingVersion;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

}
