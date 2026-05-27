package com.fukang.knowledge.agent.domain.agent.model;

/**
 * 工具信息（供 Planner 使用的摘要信息）
 * <p>仅包含 LLM 规划时需要的工具名称、描述和参数结构，不包含执行器配置等运行时细节</p>
 *
 * @param name             工具名称
 * @param description      工具描述
 * @param parametersSchema 参数 Schema（JSON 格式）
 */
public record ToolInfo(
    String name,
    String description,
    String parametersSchema
) {}