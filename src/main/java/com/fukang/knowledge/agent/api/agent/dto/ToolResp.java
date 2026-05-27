package com.fukang.knowledge.agent.api.agent.dto;

import com.fukang.knowledge.agent.common.enums.ExecutorTypeEnum;

/**
 * 工具定义响应
 *
 * @param id               工具 ID
 * @param name             工具名称
 * @param description      工具描述
 * @param executorType     执行器类型
 * @param executorConfig   执行器配置（JSON）
 * @param parametersSchema 参数 Schema
 * @param enabled          是否启用
 */
public record ToolResp(
    Long id,
    String name,
    String description,
    ExecutorTypeEnum executorType,
    String executorConfig,
    String parametersSchema,
    Boolean enabled
) {}