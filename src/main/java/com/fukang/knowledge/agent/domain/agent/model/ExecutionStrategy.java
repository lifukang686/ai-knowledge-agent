package com.fukang.knowledge.agent.domain.agent.model;

/**
 * Agent 执行策略枚举
 * <p>定义 Agent 执行任务时使用的策略模式：
 * <ul>
 *   <li>PLAN_EXECUTE — 先由 Planner 生成完整计划，再在 Reasoner 推理循环中逐步执行（适合复杂多步任务）</li>
 *   <li>AI_SERVICES — 利用 LLM 原生 Function Calling 能力，让 LLM 自主选择何时调用哪个工具（适合简单任务/自由对话）</li>
 * </ul>
 * </p>
 */
public enum ExecutionStrategy {

    /**
     * Plan-Then-Execute + ReAct 推理循环（默认策略）
     * <p>适用于需要多步编排的复杂任务</p>
     */
    PLAN_EXECUTE,

    /**
     * LLM 原生工具调用（Function Calling）
     * <p>适用于单步工具调用或自由对话场景</p>
     */
    AI_SERVICES;

    /**
     * 根据字符串解析执行策略，默认返回 PLAN_EXECUTE
     */
    public static ExecutionStrategy from(String value) {
        if (value == null || value.isBlank()) {
            return PLAN_EXECUTE;
        }
        for (ExecutionStrategy strategy : values()) {
            if (strategy.name().equalsIgnoreCase(value.trim())) {
                return strategy;
            }
        }
        return PLAN_EXECUTE;
    }
}