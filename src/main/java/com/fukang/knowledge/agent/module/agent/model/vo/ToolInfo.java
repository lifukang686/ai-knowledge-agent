package com.fukang.knowledge.agent.module.agent.model.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工具信息（供 Planner 使用的摘要信息）
 * <p>仅包含 LLM 规划时需要的工具名称、描述和参数结构，不包含执行器配置等运行时细节</p>
 *
 * @param name             工具名称
 * @param description      工具描述
 * @param parametersSchema 参数 Schema（JSON 格式）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolInfo {
    private String name;

    private String description;

    private String parametersSchema;

}