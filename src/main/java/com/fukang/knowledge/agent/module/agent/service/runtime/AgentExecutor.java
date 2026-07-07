package com.fukang.knowledge.agent.module.agent.service.runtime;

import com.fukang.knowledge.agent.module.agent.service.tool.ToolScope;
import com.fukang.knowledge.agent.module.agent.model.vo.ToolDefinition;
import com.fukang.knowledge.agent.module.agent.model.vo.Observation;
import com.fukang.knowledge.agent.module.agent.model.vo.PlanStep;
import com.fukang.knowledge.agent.module.agent.model.vo.ToolExecutionResult;
import com.fukang.knowledge.agent.module.agent.service.ToolExecutor;
import com.fukang.knowledge.agent.module.agent.service.tool.ToolExecutorFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * Agent 执行引擎
 * <p>负责按计划调用工具，将 PlanStep 转换为实际的工具调用。
 * 从本次运行的工具作用域获取工具定义，通过 ToolExecutorFactory 选择执行器，
 * 将执行结果封装为 Observation 供推理引擎使用</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentExecutor {

    private final ToolExecutorFactory executorFactory;

    /**
     * 在指定工具作用域内执行计划步骤，防止业务 Agent 调用未授权工具。
     */
    public Observation executeStep(PlanStep step, ToolScope toolScope) {
        long start = System.currentTimeMillis();

        try {
            log.info("执行步骤: step={}, tool={}, params={}",
                    step.getStepOrder(), step.getToolName(), step.getParameters());

            Optional<ToolDefinition> toolOpt = toolScope != null
                    ? toolScope.getTool(step.getToolName())
                    : Optional.empty();
            if (toolOpt.isEmpty()) {
                long duration = System.currentTimeMillis() - start;
                log.warn("工具不存在: {}", step.getToolName());
                return Observation.failure(
                        step.getStepOrder(), step.getToolName(), step.getParameters(),
                        "工具不存在: " + step.getToolName(), duration);
            }

            ToolDefinition tool = toolOpt.get();
            ToolExecutor executor = executorFactory.getExecutor(tool.getExecutorType());

            Map<String, Object> params = step.getParameters() != null ? step.getParameters() : Map.of();
            // 执行器由工具类型决定，HTTP/SQL/LOCAL_METHOD 共用同一个领域结果模型。
            ToolExecutionResult result = executor.execute(tool, params);

            long duration = System.currentTimeMillis() - start;

            if (result.isSuccess()) {
                return Observation.success(
                        step.getStepOrder(), step.getToolName(), step.getParameters(),
                        result.getOutput(), duration);
            } else {
                log.warn("步骤执行失败: step={}, tool={}, error={}",
                        step.getStepOrder(), step.getToolName(), result.getErrorMessage());
                return Observation.failure(
                        step.getStepOrder(), step.getToolName(), step.getParameters(),
                        result.getErrorMessage(), duration);
            }

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            log.error("步骤执行异常: step={}, tool={}", step.getStepOrder(), step.getToolName(), e);
            return Observation.failure(
                    step.getStepOrder(), step.getToolName(), step.getParameters(),
                    "执行异常: " + e.getMessage(), duration);
        }
    }
}
