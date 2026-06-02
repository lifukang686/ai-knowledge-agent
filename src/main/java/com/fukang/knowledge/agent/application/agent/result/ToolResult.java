package com.fukang.knowledge.agent.application.agent.result;

import com.fukang.knowledge.agent.common.enums.ExecutorTypeEnum;

/**
 * 工具定义查询结果。
 */
public record ToolResult(
        Long id,
        String name,
        String description,
        ExecutorTypeEnum executorType,
        String executorConfig,
        String parametersSchema,
        Boolean enabled
) {}
