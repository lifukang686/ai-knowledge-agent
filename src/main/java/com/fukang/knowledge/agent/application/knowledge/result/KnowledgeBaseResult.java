package com.fukang.knowledge.agent.application.knowledge.result;

import java.time.LocalDateTime;

public record KnowledgeBaseResult(
        Long id,
        String name,
        String description,
        long documentCount,
        String status,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {}
