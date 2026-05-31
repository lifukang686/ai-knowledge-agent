package com.fukang.knowledge.agent.application.agent.result;

import com.fukang.knowledge.agent.common.enums.ExecutorTypeEnum;

public record ToolResult(
        Long id,
        String name,
        String description,
        ExecutorTypeEnum executorType,
        String executorConfig,
        String parametersSchema,
        Boolean enabled
) {}
