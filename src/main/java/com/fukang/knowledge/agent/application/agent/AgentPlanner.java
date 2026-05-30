package com.fukang.knowledge.agent.application.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.enums.ModelTypeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.domain.agent.model.ToolInfo;
import com.fukang.knowledge.agent.domain.agent.model.PlanStep;
import com.fukang.knowledge.agent.infrastructure.ai.AgentMemoryFactory;
import com.fukang.knowledge.agent.infrastructure.ai.DynamicModelManager;
import com.fukang.knowledge.agent.infrastructure.ai.PromptTemplateManager;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Agent 规划引擎
 * <p>调用 LLM 将用户任务拆解为可执行的工具调用序列。
 * 使用 ReAct (Reasoning + Acting) 策略实现初始规划：
 * 将可用工具列表作为上下文输入 LLM，由 LLM 输出结构化的 JSON 执行计划</p>
 */
@Slf4j
@Component
public class AgentPlanner {

    private final DynamicModelManager dynamicModelManager;
    private final ToolRegistry toolRegistry;
    private final AgentMemoryFactory memoryFactory;
    private final PromptTemplateManager promptTemplateManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AgentPlanner(DynamicModelManager dynamicModelManager,
                        ToolRegistry toolRegistry,
                        AgentMemoryFactory memoryFactory,
                        PromptTemplateManager promptTemplateManager) {
        this.dynamicModelManager = dynamicModelManager;
        this.toolRegistry = toolRegistry;
        this.memoryFactory = memoryFactory;
        this.promptTemplateManager = promptTemplateManager;
    }

    /**
     * 根据任务描述生成执行计划
     *
     * @param task 用户自然语言任务描述
     * @return 执行计划步骤列表
     * @throws BaseException 规划生成失败或解析失败时抛出
     */
    public List<PlanStep> plan(String task) {
        List<ToolInfo> tools = toolRegistry.listAvailableTools();
        log.info("开始规划: 任务长度={}, 可用工具数={}", task.length(), tools.size());

        String toolsDesc = formatToolsForPrompt(tools);

        String jsonResponse;
        try {
            ChatLanguageModel chatModel = dynamicModelManager.getChatModel(ModelTypeEnum.CHAT);
            ChatMemory chatMemory = memoryFactory.createMessageWindowMemory(5);
            chatMemory.add(promptTemplateManager.renderSystem("agent/planning",
                    Map.of("tools", toolsDesc, "task", task)));
            chatMemory.add(UserMessage.from("请生成执行计划"));
            Response<AiMessage> response = chatModel.generate(chatMemory.messages());
            chatMemory.add(response.content());
            jsonResponse = extractJson(response.content().text());
            log.debug("LLM 规划响应: {}", jsonResponse);
        } catch (Exception e) {
            log.error("LLM 规划调用失败", e);
            throw new BaseException(ErrorCodeEnum.AGENT_PLANNING_FAILED);
        }

        return parsePlanSteps(jsonResponse);
    }

    /**
     * 解析 LLM 返回的计划 JSON
     */
    List<PlanStep> parsePlanSteps(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode stepsNode = root.get("steps");
            if (stepsNode == null || !stepsNode.isArray()) {
                log.error("计划 JSON 缺少 steps 数组: {}", jsonResponse);
                throw new BaseException(ErrorCodeEnum.AGENT_PLAN_PARSE_FAILED);
            }

            List<PlanStep> steps = new ArrayList<>();
            for (JsonNode stepNode : stepsNode) {
                int stepOrder = stepNode.get("stepOrder").asInt();
                String toolName = stepNode.get("toolName").asText();
                Map<String, Object> parameters = objectMapper.convertValue(
                        stepNode.get("parameters"),
                        new TypeReference<Map<String, Object>>() {}
                );
                String reasoning = stepNode.has("reasoning") ? stepNode.get("reasoning").asText() : "";
                steps.add(new PlanStep(stepOrder, toolName, parameters, reasoning));
            }

            log.info("规划解析完成: 步骤数={}", steps.size());
            return steps;
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("解析计划 JSON 失败: {}", jsonResponse, e);
            throw new BaseException(ErrorCodeEnum.AGENT_PLAN_PARSE_FAILED);
        }
    }

    /**
     * 从 LLM 响应中提取 JSON 内容
     * <p>处理 LLM 可能在 JSON 前后添加 markdown 代码块标记的情况</p>
     */
    private String extractJson(String text) {
        if (text == null || text.isBlank()) {
            return "{}";
        }
        String trimmed = text.trim();
        if (trimmed.startsWith("```json")) {
            int end = trimmed.lastIndexOf("```");
            if (end > 7) {
                return trimmed.substring(7, end).trim();
            }
        }
        if (trimmed.startsWith("```")) {
            int end = trimmed.lastIndexOf("```");
            if (end > 3) {
                return trimmed.substring(3, end).trim();
            }
        }
        return trimmed;
    }

    /**
     * 格式化工具列表为 LLM 提示文本
     */
    private String formatToolsForPrompt(List<ToolInfo> tools) {
        if (tools.isEmpty()) {
            return "（暂无可用工具）";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tools.size(); i++) {
            ToolInfo tool = tools.get(i);
            sb.append(String.format("%d. 工具名: %s\n", i + 1, tool.name()));
            sb.append(String.format("   描述: %s\n", tool.description()));
            sb.append(String.format("   参数: %s\n", tool.parametersSchema()));
        }
        return sb.toString();
    }
}