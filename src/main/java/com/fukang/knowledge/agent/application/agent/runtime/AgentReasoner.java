package com.fukang.knowledge.agent.application.agent.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fukang.knowledge.agent.application.agent.port.AgentChatClient;
import com.fukang.knowledge.agent.domain.agent.model.AgentChatMessage;
import com.fukang.knowledge.agent.domain.agent.model.AgentChatSession;
import com.fukang.knowledge.agent.domain.agent.model.AgentContext;
import com.fukang.knowledge.agent.domain.agent.model.AgentStep;
import com.fukang.knowledge.agent.domain.agent.model.PlanStep;
import com.fukang.knowledge.agent.domain.agent.model.ReasoningResult;
import com.fukang.knowledge.agent.infrastructure.ai.PromptTemplateManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Agent 推理引擎
 * <p>将工具执行的观察结果反馈给 LLM，判断 Agent 下一步应该执行的动作。
 * 支持四种决策类型：
 * <ul>
 *   <li>CONTINUE — 继续执行下一个计划步骤</li>
 *   <li>FINAL_ANSWER — 任务完成，content 为整合后的最终答案</li>
 *   <li>RETRY — 重试上一步失败的操作</li>
 *   <li>ABORT — 终止执行，content 为终止原因</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
public class AgentReasoner {

    private static final String REASONING_SYSTEM_PROMPT = """
            你是一个任务执行推理助手。你正在帮助用户完成一个任务，已经执行了一些步骤。
            
            请判断下一步应该做什么，按以下 JSON 格式返回（仅返回 JSON，不要包含 markdown 标记或其他内容）：
            {
              "decision": "CONTINUE|FINAL_ANSWER|RETRY|ABORT",
              "content": "..."
            }
            
            决策规则：
            1. CONTINUE — 上一步成功且还有未执行步骤，content 为空即可（系统将按计划继续）
            2. FINAL_ANSWER — 满足以下任一条件：
               - 所有步骤已成功执行
               - 已获得足够信息回答用户的问题
               content 应为一段自然语言，综合所有步骤的结果直接回答用户的原始问题
            3. RETRY — 上一步失败但错误可恢复（如网络超时），content 说明重试原因
            4. ABORT — 上一步失败的不可恢复错误（如参数错误、资源不存在），content 说明终止原因""";

    private final AgentChatClient agentChatClient;
    private final PromptTemplateManager promptTemplateManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AgentReasoner(AgentChatClient agentChatClient,
                         PromptTemplateManager promptTemplateManager) {
        this.agentChatClient = agentChatClient;
        this.promptTemplateManager = promptTemplateManager;
    }

    /**
     * 根据 Agent 执行上下文进行推理决策
     *
     * @param context Agent 执行上下文（含原始任务、历史步骤）
     * @return 推理结果
     */
    public ReasoningResult reason(AgentContext context) {
        log.debug("开始推理: completedSteps={}/{}, totalSteps={}",
                context.getCompletedStepCount(), context.getTotalStepCount(),
                context.getTotalStepCount());

        AgentStep lastStep = context.getLastStep();
        String stepsHistory = formatStepsHistory(context.getSteps());
        String remainingSteps = formatRemainingSteps(context.getRemainingSteps());
        String lastStepDetail = formatLastStepDetail(lastStep);
        boolean lastStepSuccess = lastStep != null && Boolean.TRUE.equals(lastStep.success());

        String userPrompt = promptTemplateManager.renderText("agent/reasoning", Map.of(
                "originalTask", context.getTask(),
                "stepsHistory", stepsHistory,
                "completedSteps", String.valueOf(context.getCompletedStepCount()),
                "totalSteps", String.valueOf(context.getTotalStepCount()),
                "remainingSteps", remainingSteps,
                "lastStepSuccess", String.valueOf(lastStepSuccess),
                "lastStepDetail", lastStepDetail
        ));

        AgentChatSession chatSession = context.getChatSession();
        if (chatSession == null) {
            // Reasoner 复用同一个会话，保留推理上下文，减少每轮都重新解释任务状态。
            chatSession = agentChatClient.newDefaultSession();
            context.setChatSession(chatSession);
            chatSession.add(AgentChatMessage.system(REASONING_SYSTEM_PROMPT));
        }

        String jsonResponse;
        try {
            String responseText = agentChatClient.generate(chatSession, List.of(AgentChatMessage.user(userPrompt)));
            jsonResponse = extractJson(responseText);
            log.debug("LLM 推理响应: {}", jsonResponse);
            return parseReasoningResult(jsonResponse);
        } catch (Exception e) {
            log.error("LLM 推理调用失败，降级处理", e);
            return fallbackReason(context);
        }
    }

    /**
     * 降级推理：不调用 LLM，基于规则判断
     * <p>当 LLM 推理不可用时使用简单规则做决策</p>
     */
    private ReasoningResult fallbackReason(AgentContext context) {
        AgentStep lastStep = context.getLastStep();
        boolean hasRemainingSteps = !context.getRemainingSteps().isEmpty();

        if (lastStep == null) {
            if (hasRemainingSteps) {
                return new ReasoningResult(ReasoningResult.Decision.CONTINUE, "");
            }
            return new ReasoningResult(ReasoningResult.Decision.ABORT, "无已执行步骤且无剩余步骤，任务无法继续");
        }

        if (Boolean.TRUE.equals(lastStep.success())) {
            if (hasRemainingSteps) {
                return new ReasoningResult(ReasoningResult.Decision.CONTINUE, "");
            }
            return buildFallbackFinalAnswer(context);
        }

        String error = lastStep.errorMessage() != null ? lastStep.errorMessage() : "";
        if (error.contains("超时") || error.contains("timeout") || error.contains("timed out")) {
            return new ReasoningResult(ReasoningResult.Decision.RETRY, "上一步执行超时，建议重试");
        }
        return new ReasoningResult(ReasoningResult.Decision.ABORT, "上一步执行失败: " + error);
    }

    /**
     * 构建降级最终答案：直接拼接所有步骤的观察结果
     */
    private ReasoningResult buildFallbackFinalAnswer(AgentContext context) {
        if (context.getSteps().isEmpty()) {
            return new ReasoningResult(ReasoningResult.Decision.ABORT, "任务未执行任何步骤");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("根据以下执行结果，为您汇总：\n\n");
        for (AgentStep step : context.getSteps()) {
            sb.append(String.format("【步骤 %d - %s】\n", step.stepOrder(), step.toolName()));
            if (Boolean.TRUE.equals(step.success())) {
                sb.append(step.observation()).append("\n\n");
            } else {
                sb.append("执行失败: ").append(step.errorMessage()).append("\n\n");
            }
        }
        return new ReasoningResult(ReasoningResult.Decision.FINAL_ANSWER, sb.toString().trim());
    }

    private String formatStepsHistory(List<AgentStep> steps) {
        if (steps.isEmpty()) {
            return "（尚无已执行步骤）";
        }
        StringBuilder sb = new StringBuilder();
        for (AgentStep step : steps) {
            sb.append(String.format("步骤%d [%s]: 工具='%s', 参数=%s\n",
                    step.stepOrder(),
                    Boolean.TRUE.equals(step.success()) ? "成功" : "失败",
                    step.toolName(),
                    step.parameters()));
            String obs = step.observation();
            if (obs != null && obs.length() > 500) {
                obs = obs.substring(0, 500) + "...(已截断)";
            }
            sb.append(String.format("  结果: %s (耗时 %dms)\n",
                    obs != null ? obs : "(无输出)",
                    step.durationMs() != null ? step.durationMs() : 0));
        }
        return sb.toString();
    }

    private String formatRemainingSteps(List<PlanStep> steps) {
        if (steps.isEmpty()) {
            return "（无剩余步骤）";
        }
        return steps.stream()
                .map(s -> String.format("  - 步骤%d: 工具 '%s', 说明: %s",
                        s.stepOrder(), s.toolName(),
                        s.reasoning() != null ? s.reasoning() : ""))
                .collect(Collectors.joining("\n"));
    }

    private String formatLastStepDetail(AgentStep lastStep) {
        if (lastStep == null) {
            return "（首个步骤，无上一步）";
        }
        String obs = lastStep.observation();
        if (obs != null && obs.length() > 300) {
            obs = obs.substring(0, 300) + "...(已截断)";
        }
        return String.format("工具: %s, 耗时: %dms, 错误: %s, 结果: %s",
                lastStep.toolName(),
                lastStep.durationMs() != null ? lastStep.durationMs() : 0,
                lastStep.errorMessage() != null ? lastStep.errorMessage() : "无",
                obs != null ? obs : "(无输出)");
    }

    /**
     * 解析 LLM 返回的推理 JSON
     */
    ReasoningResult parseReasoningResult(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            String decision = root.get("decision").asText();
            String content = root.has("content") ? root.get("content").asText() : "";
            return new ReasoningResult(ReasoningResult.Decision.valueOf(decision.toUpperCase()), content);
        } catch (Exception e) {
            log.error("解析推理结果 JSON 失败: {}", jsonResponse, e);
            return new ReasoningResult(ReasoningResult.Decision.ABORT,
                    "推理结果解析失败，JSON: " + jsonResponse);
        }
    }

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
}
