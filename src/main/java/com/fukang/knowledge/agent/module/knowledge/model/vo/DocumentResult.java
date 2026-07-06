package com.fukang.knowledge.agent.module.knowledge.model.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 文档列表项查询结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResult {
    private Long id;

    private String title;

    private String filePath;

    private Long knowledgeBaseId;

    private String status;

    private String uploadedBy;

    private long chunkCount;

    private long fileSize;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

}
