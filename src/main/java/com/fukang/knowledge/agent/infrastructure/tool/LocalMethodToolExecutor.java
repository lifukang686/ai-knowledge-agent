package com.fukang.knowledge.agent.infrastructure.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fukang.knowledge.agent.domain.agent.model.ToolDefinition;
import com.fukang.knowledge.agent.domain.agent.model.ToolExecutionResult;
import com.fukang.knowledge.agent.domain.agent.service.ToolExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * LOCAL_METHOD 工具执行器。
 * <p>将 Agent 工具调用路由到受控的 Spring Bean 工具，而不是开放任意方法反射。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocalMethodToolExecutor implements ToolExecutor {

    private final LocalMethodToolRegistry localMethodToolRegistry;
    private final ObjectMapper objectMapper;

    @Override
    public ToolExecutionResult execute(ToolDefinition tool, Map<String, Object> parameters) {
        long start = System.currentTimeMillis();
        try {
            LocalMethodTool localTool = localMethodToolRegistry.get(tool.name())
                    .orElseThrow(() -> new IllegalArgumentException("本地方法工具未注册: " + tool.name()));
            Object output = localTool.execute(parameters != null ? parameters : Map.of());
            return ToolExecutionResult.success(toJson(output), System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.warn("本地方法工具执行失败: tool={}", tool.name(), e);
            return ToolExecutionResult.failure(e.getMessage(), System.currentTimeMillis() - start);
        }
    }

    private String toJson(Object output) throws Exception {
        if (output == null) {
            return "{}";
        }
        if (output instanceof String text) {
            return text;
        }
        return objectMapper.writeValueAsString(output);
    }
}
