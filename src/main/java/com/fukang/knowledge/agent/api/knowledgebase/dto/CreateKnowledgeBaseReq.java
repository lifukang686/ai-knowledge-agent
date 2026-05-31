package com.fukang.knowledge.agent.api.knowledgebase.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 创建知识库请求 DTO
 *
 * @param name        知识库名称，不能为空，1-100个字符
 * @param description 知识库描述，可选，最多500个字符
 */
public record CreateKnowledgeBaseReq(
        @NotBlank(message = "知识库名称不能为空")
        String name,
        String description
) {}
