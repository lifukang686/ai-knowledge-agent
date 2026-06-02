package com.fukang.knowledge.agent.application.knowledge.result;

import java.time.LocalDateTime;

/**
 * 知识库查询结果，包含文档数量统计。
 */
public record KnowledgeBaseResult(
        Long id,
        String name,
        String description,
        long documentCount,
        String status,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {}
