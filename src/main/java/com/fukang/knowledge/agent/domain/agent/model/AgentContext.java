package com.fukang.knowledge.agent.domain.agent.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent 执行上下文
 * <p>管理单次 Agent 任务执行的全部状态信息。
 * 包括原始任务描述、执行计划、历史步骤、当前状态和 Token 消耗统计</p>
 */
@Data
public class AgentContext {

    /** 原始任务描述 */
    private final String task;

    /** 执行计划中的所有步骤 */
    private List<PlanStep> planSteps = List.of();

    /** 已执行步骤的历史记录 */
    private List<AgentStep> steps = new ArrayList<>();

    /** 当前执行状态 */
    private AgentContextStatus status = AgentContextStatus.PLANNING;

    /** 累计 Token 消耗 */
    private int totalTokens;

    public AgentContext(String task) {
        this.task = task;
    }

    /**
     * 获取已完成步骤数量
     */
    public int getCompletedStepCount() {
        return steps.size();
    }

    /**
     * 获取计划总步骤数
     */
    public int getTotalStepCount() {
        return planSteps != null ? planSteps.size() : 0;
    }

    /**
     * 获取尚未执行的步骤列表
     */
    public List<PlanStep> getRemainingSteps() {
        if (planSteps == null || planSteps.isEmpty()) {
            return List.of();
        }
        int completed = steps.size();
        if (completed >= planSteps.size()) {
            return List.of();
        }
        return planSteps.subList(completed, planSteps.size());
    }

    /**
     * 获取上一步的执行结果（用于推理）
     */
    public AgentStep getLastStep() {
        if (steps.isEmpty()) {
            return null;
        }
        return steps.get(steps.size() - 1);
    }

    /**
     * 添加一次步骤执行记录
     */
    public void addStep(AgentStep step) {
        steps.add(step);
    }

    /**
     * 累加 Token 消耗
     */
    public void addTokenUsage(int tokens) {
        this.totalTokens += tokens;
    }

    /**
     * Agent 上下文状态枚举
     */
    public enum AgentContextStatus {
        PLANNING,
        EXECUTING,
        COMPLETED,
        FAILED,
        CANCELLED
    }
}