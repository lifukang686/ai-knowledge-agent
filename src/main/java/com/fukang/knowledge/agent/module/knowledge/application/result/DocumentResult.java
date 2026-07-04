package com.fukang.knowledge.agent.module.knowledge.application.result;

import java.time.LocalDateTime;

/**
 * 文档列表项查询结果。
 */
public record DocumentResult(
        Long id,
        String title,
        String filePath,
        Long knowledgeBaseId,
        String status,
        String uploadedBy,
        long chunkCount,
        long fileSize,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {}
