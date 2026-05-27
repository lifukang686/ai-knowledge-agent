package com.fukang.knowledge.agent.agent.planning;

import java.util.Map;

/**
 * 计划步骤值对象
 * <p>由 Planner 调用 LLM 生成，描述 Agent 需要执行的单个工具调用步骤</p>
 *
 * @param stepOrder  步骤序号（从 1 开始）
 * @param toolName   要调用的工具名称
 * @param parameters 工具调用参数
 * @param reasoning  LLM 给出的该步骤必要性说明
 */
public record PlanStep(
    Integer stepOrder,
    String toolName,
    Map<String, Object> parameters,
    String reasoning
) {}