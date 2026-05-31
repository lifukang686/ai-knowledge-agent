package com.fukang.knowledge.agent.application.knowledge.result;

import java.time.LocalDateTime;

public record DocumentDetailResult(
        Long id,
        String title,
        String content,
        String filePath,
        Long knowledgeBaseId,
        String status,
        String uploadedBy,
        long chunkCount,
        long fileSize,
        Long embeddingModelId,
        Integer embeddingDimension,
        String embeddingVersion,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {}
