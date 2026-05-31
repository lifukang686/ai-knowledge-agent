package com.fukang.knowledge.agent.infrastructure.tool;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fukang.knowledge.agent.application.agent.AgentRunEventCollector;
import com.fukang.knowledge.agent.application.agent.ToolRegistry;
import com.fukang.knowledge.agent.domain.agent.model.AgentRunEvent;
import com.fukang.knowledge.agent.domain.agent.model.ToolDefinition;
import com.fukang.knowledge.agent.domain.agent.model.ToolExecutionResult;
import com.fukang.knowledge.agent.domain.agent.model.ToolInfo;
import com.fukang.knowledge.agent.domain.agent.service.ToolExecutor;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 动态工具提供器
 * <p>实现 LangChain4j 的 {@link ToolProvider} 接口，将 ToolRegistry 中页面配置化的
 * 工具定义实时转换为 {@link ToolSpecification}，由 AiServices 框架自动完成
 * ReAct（Thought → Action → Observation）循环中的工具识别与调用。
 *
 * <pre>
 * AiServices 调用流程：
 * 1. AiServices 调用 provideTools() 获取当前可用工具列表
 * 2. LLM 决定调用某个工具 → AiServices 通过 ToolExecutor 执行
 * 3. 工具结果自动反馈给 LLM → LLM 决定继续调用还是输出答案
 * 4. 每次 userMessage 变化时，provideTools() 会被重新调用，确保工具列表实时
 * </pre>
 * </p>
 */
@Slf4j
@Component
public class DynamicToolProvider implements ToolProvider {

    private final ToolRegistry toolRegistry;
    private final ToolExecutorFactory executorFactory;
    private final ToolSchemaConverter toolSchemaConverter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DynamicToolProvider(ToolRegistry toolRegistry,
                               ToolExecutorFactory executorFactory,
                               ToolSchemaConverter toolSchemaConverter) {
        this.toolRegistry = toolRegistry;
        this.executorFactory = executorFactory;
        this.toolSchemaConverter = toolSchemaConverter;
    }

    /**
     * 由 AiServices 框架调用，提供当前可用的工具列表
     * <p>每次调用都会从 ToolRegistry 实时获取最新工具，返回 ToolSpecification 与
     * ToolExecutor 的映射，框架自动处理工具识别、调用、结果反馈的 ReAct 循环</p>
     */
    @Override
    public ToolProviderResult provideTools(ToolProviderRequest request) {
        List<ToolInfo> tools = toolRegistry.listAvailableTools();
        log.debug("动态工具提供器: 获取到 {} 个可用工具", tools.size());

        Map<ToolSpecification, dev.langchain4j.service.tool.ToolExecutor> toolMap = new LinkedHashMap<>();

        for (ToolInfo info : tools) {
            ToolSpecification spec = ToolSpecification.builder()
                    .name(info.name())
                    .description(info.description())
                    .parameters(toolSchemaConverter.fromJsonSchema(info.parametersSchema()))
                    .build();

            dev.langchain4j.service.tool.ToolExecutor lc4jExecutor =
                    (toolRequest, memoryId) -> executeTool(toolRequest);

            toolMap.put(spec, lc4jExecutor);
        }

        return new ToolProviderResult(toolMap);
    }

    /**
     * 执行指定工具（由 AiServices 框架自动路由调用）
     */
    private String executeTool(ToolExecutionRequest request) {
        String toolName = request.name();
        log.info("AiServices 工具调用: tool={}, args={}", toolName, request.arguments());

        Optional<ToolDefinition> toolOpt = toolRegistry.getTool(toolName);
        if (toolOpt.isEmpty()) {
            String error = "工具 '" + toolName + "' 不存在或已禁用";
            log.warn("工具执行失败: {}", error);
            return error;
        }

        ToolDefinition tool = toolOpt.get();
        ToolExecutor executor = executorFactory.getExecutor(tool.executorType());
        Map<String, Object> params = parseArguments(request.arguments());
        AgentRunEventCollector.record(AgentRunEvent.of(
                AgentRunEvent.EventType.TOOL_CALL, null, toolName,
                Map.of("arguments", params), null, null, "AiServices tool call"));

        long start = System.currentTimeMillis();
        ToolExecutionResult result = executor.execute(tool, params);
        long duration = System.currentTimeMillis() - start;
        AgentRunEventCollector.record(AgentRunEvent.of(
                AgentRunEvent.EventType.OBSERVATION, null, toolName,
                Map.of("output", result.output() != null ? result.output() : "",
                        "errorMessage", result.errorMessage() != null ? result.errorMessage() : ""),
                result.success(), duration,
                result.success() ? "Tool execution succeeded" : "Tool execution failed"));

        if (result.success()) {
            log.info("工具执行成功: tool={}, duration={}ms", toolName, duration);
            return result.output();
        } else {
            log.warn("工具执行失败: tool={}, error={}", toolName, result.errorMessage());
            return "工具执行失败: " + result.errorMessage();
        }
    }

    private Map<String, Object> parseArguments(String arguments) {
        if (arguments == null || arguments.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(arguments,
                    new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("解析工具参数失败: {}", arguments, e);
            return Map.of();
        }
    }
}
