package com.fukang.knowledge.agent.api.knowledgebase.dto;

/**
 * 更新知识库请求 DTO
 * <p>仅更新非空字段，未传字段保持原值不变</p>
 *
 * @param name        知识库名称，可选
 * @param description 知识库描述，可选
 */
public record UpdateKnowledgeBaseReq(
        String name,
        String description
) {}
