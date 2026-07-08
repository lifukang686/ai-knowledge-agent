package com.fukang.knowledge.agent.module.agent.model.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

/**
 * 计划步骤值对象
 * <p>由 Planner 调用 LLM 生成，描述 Agent 需要执行的单个工具调用步骤</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanStep {
    /** 步骤序号（从 1 开始） */
    private Integer stepOrder;

    /** 要调用的工具名称 */
    private String toolName;

    /** 工具调用参数 */
    private Map<String, Object> parameters;

    /** LLM 给出的该步骤必要性说明 */
    private String reasoning;

}