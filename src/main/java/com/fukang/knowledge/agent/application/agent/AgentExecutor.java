package com.fukang.knowledge.agent.application.agent;

import com.fukang.knowledge.agent.domain.agent.model.ToolDefinition;
import com.fukang.knowledge.agent.domain.agent.model.Observation;
import com.fukang.knowledge.agent.domain.agent.model.PlanStep;
import com.fukang.knowledge.agent.domain.agent.service.ToolExecutor;
import com.fukang.knowledge.agent.infrastructure.tool.ToolExecutorFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * Agent 执行引擎
 * <p>负责按计划调用工具，将 PlanStep 转换为实际的工具调用。
 * 从 ToolRegistry 获取工具定义，通过 ToolExecutorFactory 选择执行器，
 * 将执行结果封装为 Observation 供推理引擎使用</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentExecutor {

    private final ToolRegistry toolRegistry;
    private final ToolExecutorFactory executorFactory;

    /**
     * 执行单个计划步骤
     *
     * @param step 计划步骤（含工具名和参数）
     * @return 观察结果（封装执行成功/失败、输出和耗时）
     */
    public Observation executeStep(PlanStep step) {
        return executeStep(step, toolRegistry);
    }

    /**
     * 在指定工具作用域内执行计划步骤，防止业务 Agent 调用未授权工具。
     */
    public Observation executeStep(PlanStep step, ToolScope toolScope) {
        long start = System.currentTimeMillis();

        try {
            log.info("执行步骤: step={}/{}, tool={}, params={}",
                    step.stepOrder(), step.toolName(), step.parameters());

            ToolScope scope = toolScope != null ? toolScope : toolRegistry;
            Optional<ToolDefinition> toolOpt = scope.getTool(step.toolName());
            if (toolOpt.isEmpty()) {
                long duration = System.currentTimeMillis() - start;
                log.warn("工具不存在: {}", step.toolName());
                return Observation.failure(
                        step.stepOrder(), step.toolName(), step.parameters(),
                        "工具不存在: " + step.toolName(), duration);
            }

            ToolDefinition tool = toolOpt.get();
            ToolExecutor executor = executorFactory.getExecutor(tool.executorType());

            Map<String, Object> params = step.parameters() != null ? step.parameters() : Map.of();
            com.fukang.knowledge.agent.domain.agent.model.ToolExecutionResult result = executor.execute(tool, params);

            long duration = System.currentTimeMillis() - start;

            if (result.success()) {
                return Observation.success(
                        step.stepOrder(), step.toolName(), step.parameters(),
                        result.output(), duration);
            } else {
                log.warn("步骤执行失败: step={}, tool={}, error={}",
                        step.stepOrder(), step.toolName(), result.errorMessage());
                return Observation.failure(
                        step.stepOrder(), step.toolName(), step.parameters(),
                        result.errorMessage(), duration);
            }

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            log.error("步骤执行异常: step={}, tool={}", step.stepOrder(), step.toolName(), e);
            return Observation.failure(
                    step.stepOrder(), step.toolName(), step.parameters(),
                    "执行异常: " + e.getMessage(), duration);
        }
    }
}
