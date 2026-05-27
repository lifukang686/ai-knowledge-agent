package com.fukang.knowledge.agent.api.agent.dto;

/**
 * Agent 配置响应
 *
 * @param id           Agent ID
 * @param name         Agent 名称
 * @param description  Agent 描述
 * @param toolIds      关联工具 ID 列表
 * @param systemPrompt 系统提示词模板
 * @param maxSteps     最大执行步数
 * @param createTime   创建时间
 */
public record AgentResp(
    Long id,
    String name,
    String description,
    java.util.List<Long> toolIds,
    String systemPrompt,
    Integer maxSteps,
    String createTime
) {}