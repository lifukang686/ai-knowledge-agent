package com.fukang.knowledge.agent.api.knowledgebase.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 知识库响应 DTO
 * <p>返回给前端的知识库完整信息，包含基础信息和统计字段</p>
 *
 * @param id            知识库ID（Long→String 序列化）
 * @param name          知识库名称
 * @param description   知识库描述
 * @param documentCount 文档数量
 * @param status        知识库状态
 * @param createTime    创建时间（格式化: yyyy-MM-dd HH:mm:ss）
 * @param updateTime    更新时间（格式化: yyyy-MM-dd HH:mm:ss）
 */
public record KnowledgeBaseResp(
        Long id,
        String name,
        String description,
        long documentCount,
        String status,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        LocalDateTime createTime,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        LocalDateTime updateTime
) {}
