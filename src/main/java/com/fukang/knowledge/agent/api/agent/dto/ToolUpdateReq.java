package com.fukang.knowledge.agent.api.agent.dto;

import com.fukang.knowledge.agent.common.enums.ExecutorTypeEnum;

/**
 * 工具更新请求
 *
 * @param name             工具名称（唯一）
 * @param description      工具描述
 * @param executorType     执行器类型
 * @param executorConfig   执行器配置（JSON）
 * @param parametersSchema 参数 Schema（JSON）
 */
public record ToolUpdateReq(
    String name,
    String description,
    ExecutorTypeEnum executorType,
    String executorConfig,
    String parametersSchema
) {}