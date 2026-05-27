package com.fukang.knowledge.agent.agent.execution;

import com.fukang.knowledge.agent.domain.agent.model.ToolDefinition;

import java.util.Map;

/**
 * 工具执行器接口
 * <p>定义统一的工具执行契约，不同实现类对应不同的执行器类型（HTTP / SQL / LOCAL_METHOD）。
 * 所有执行器实现此接口，由 ToolExecutorFactory 根据 executorType 选择对应实现</p>
 */
public interface ToolExecutor {

    /**
     * 执行工具并返回结果
     *
     * @param toolDefinition 工具定义（含执行器类型和配置）
     * @param parameters     调用参数（由 LLM 规划阶段生成）
     * @return 工具执行结果
     */
    ToolExecutionResult execute(ToolDefinition toolDefinition, Map<String, Object> parameters);
}