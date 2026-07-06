package com.fukang.knowledge.agent.module.agent.model.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fukang.knowledge.agent.common.enums.ExecutorTypeEnum;

/**
 * 工具定义领域对象
 * <p>描述 Agent 可调用的工具属性，包括名称、描述、执行器类型和参数 Schema</p>
 *
 * @param name             工具名称（唯一标识）
 * @param description      工具功能描述（供 LLM 理解）
 * @param executorType     执行器类型
 * @param executorConfig   执行器配置（JSON 格式）
 * @param parametersSchema 参数 Schema（JSON 格式）
 * @param enabled          是否启用
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolDefinition {
    private Long id;

    private String name;

    private String description;

    private ExecutorTypeEnum executorType;

    private String executorConfig;

    private String parametersSchema;

    private Boolean enabled;

}