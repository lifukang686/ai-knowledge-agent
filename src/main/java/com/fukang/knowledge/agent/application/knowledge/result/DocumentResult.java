package com.fukang.knowledge.agent.application.knowledge.result;

import java.time.LocalDateTime;

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
