package com.fukang.knowledge.agent.application.knowledge.result;

import java.time.LocalDateTime;

public record KnowledgeBaseResult(
        Long id,
        String name,
        String description,
        long documentCount,
        String status,
        Long embeddingModelId,
        Integer embeddingDimension,
        String embeddingVersion,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {}
