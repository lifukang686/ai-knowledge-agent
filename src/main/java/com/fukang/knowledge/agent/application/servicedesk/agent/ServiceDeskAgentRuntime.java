package com.fukang.knowledge.agent.application.servicedesk.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fukang.knowledge.agent.application.agent.runtime.AgentRuntimeOptions;
import com.fukang.knowledge.agent.application.agent.runtime.PlanExecuteAgentRuntime;
import com.fukang.knowledge.agent.application.agent.tool.ScopedToolRegistry;
import com.fukang.knowledge.agent.application.servicedesk.command.ServiceDeskAskCommand;
import com.fukang.knowledge.agent.application.servicedesk.result.ServiceDeskAnswerResult;
import com.fukang.knowledge.agent.application.servicedesk.result.ServiceTicketResult;
import com.fukang.knowledge.agent.domain.agent.model.AgentRunEvent;
import com.fukang.knowledge.agent.domain.agent.model.AgentStep;
import com.fukang.knowledge.agent.domain.servicedesk.ServiceType;
import com.fukang.knowledge.agent.domain.servicedesk.TicketStatus;
import com.fukang.knowledge.agent.infrastructure.ai.PromptTemplateManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 服务台专用 Plan-Execute Agent Runtime。
 * <p>将服务台请求交给 LLM 规划工具调用，同时通过工具作用域和 DRAFT 策略保持业务可控。</p>
 */
@Component
@RequiredArgsConstructor
public class ServiceDeskAgentRuntime {

    private static final int MAX_STEPS = 4;
    private static final String PLANNING_PROMPT = "service-desk/agent-planning.v1";

    private final PlanExecuteAgentRuntime planExecuteAgentRuntime;
    private final ServiceDeskAgentToolFactory toolFactory;
    private final PromptTemplateManager promptTemplateManager;
    private final ObjectMapper objectMapper;

    /**
     * 在服务台受控工具集合内执行一次用户问题处理。
     */
    public ServiceDeskAnswerResult run(ServiceDeskAskCommand command, Long userId, Long runId) {
        ServiceType serviceType = ServiceType.from(command.serviceType());
        ServiceDeskAgentContext context = new ServiceDeskAgentContext(
                userId, runId, command.knowledgeBaseId(), command.conversationId(),
                serviceType, command.question());
        ScopedToolRegistry toolScope = toolFactory.createScope();
        String planningPrompt = promptTemplateManager.renderText(PLANNING_PROMPT, Map.of(
                "serviceType", serviceType.name(),
                "knowledgeBaseId", command.knowledgeBaseId() != null ? String.valueOf(command.knowledgeBaseId()) : "",
                "conversationId", command.conversationId() != null ? String.valueOf(command.conversationId()) : ""
        ));

        try {
            ServiceDeskAgentContextHolder.set(context);
            PlanExecuteAgentRuntime.RuntimeResult result = planExecuteAgentRuntime.runTask(
                    command.question(), AgentRuntimeOptions.of(MAX_STEPS, planningPrompt, toolScope));
            return toAnswerResult(result, runId, serviceType.name());
        } finally {
            ServiceDeskAgentContextHolder.clear();
        }
    }

    private ServiceDeskAnswerResult toAnswerResult(PlanExecuteAgentRuntime.RuntimeResult result,
                                                   Long runId, String serviceType) {
        ToolOutcome outcome = extractOutcome(result.steps());
        String answer = result.answer();
        if (outcome.approvalRequired()) {
            // 写操作只能生成草稿，最终话术以工具事实为准，防止模型误说“已正式创建”。
            answer = outcome.answer();
        } else if (answer == null || answer.isBlank()) {
            answer = outcome.answer() != null ? outcome.answer() : "服务台 Agent 已完成处理。";
        }
        return new ServiceDeskAnswerResult(
                answer,
                outcome.intent(),
                serviceType,
                outcome.status() != null ? outcome.status() : normalizeStatus(result.status()),
                runId,
                outcome.ticketId(),
                outcome.ticketNo(),
                outcome.conversationId(),
                outcome.approvalRequired(),
                outcome.pendingTicket(),
                result.events(),
                false
        );
    }

    private ToolOutcome extractOutcome(List<AgentStep> steps) {
        ToolOutcome outcome = ToolOutcome.empty();
        for (AgentStep step : steps) {
            Map<String, Object> output = parseObservation(step.observation());
            // 多个工具结果按执行顺序合并，后续写操作结果优先覆盖普通问答结果。
            outcome = outcome.merge(toolOutcome(step.toolName(), output));
        }
        return outcome;
    }

    private ToolOutcome toolOutcome(String toolName, Map<String, Object> output) {
        if (ServiceDeskToolNames.KNOWLEDGE_QA.equals(toolName)) {
            return new ToolOutcome("knowledge_qa", text(output, "answer"), text(output, "status"),
                    null, null, longValue(output.get("conversationId")), false, null);
        }
        if (ServiceDeskToolNames.DRAFT_TICKET.equals(toolName)) {
            return ticketOutcome("create_ticket", output);
        }
        if (ServiceDeskToolNames.REQUEST_HUMAN_HANDOFF.equals(toolName)) {
            return ticketOutcome("handoff_human", output);
        }
        if (ServiceDeskToolNames.QUERY_TICKET.equals(toolName)) {
            return queryOutcome(output);
        }
        if (ServiceDeskToolNames.ASK_FOR_MORE_INFO.equals(toolName)) {
            return new ToolOutcome("collect_info", text(output, "message"), "collect_info",
                    null, null, null, false, null);
        }
        return ToolOutcome.empty();
    }

    private ToolOutcome ticketOutcome(String intent, Map<String, Object> output) {
        Long ticketId = longValue(output.get("ticketId"));
        String ticketNo = text(output, "ticketNo");
        ServiceTicketResult pendingTicket = new ServiceTicketResult(
                ticketId,
                ticketNo,
                text(output, "serviceType"),
                text(output, "category"),
                text(output, "priority"),
                text(output, "status", TicketStatus.DRAFT.name()),
                text(output, "title"),
                null,
                text(output, "summary"),
                null,
                null,
                null,
                null,
                List.of(),
                0L,
                null,
                null
        );
        String answer = "我已生成服务台工单草稿：" + ticketNo + "。请确认后提交，确认前不会正式打开工单。";
        if ("handoff_human".equals(intent)) {
            answer = "该问题需要人工介入。我已生成高优先级工单草稿：" + ticketNo + "。请确认后提交服务台。";
        }
        return new ToolOutcome(intent, answer, "success", ticketId, ticketNo, null, true, pendingTicket);
    }

    private ToolOutcome queryOutcome(Map<String, Object> output) {
        Object ticket = output.get("ticket");
        if (ticket instanceof Map<?, ?> map) {
            Long ticketId = longValue(map.get("ticketId"));
            String ticketNo = Objects.toString(map.get("ticketNo"), "");
            String status = Objects.toString(map.get("status"), "");
            String title = Objects.toString(map.get("title"), "");
            return new ToolOutcome("query_ticket",
                    "工单 " + ticketNo + " 当前状态为 " + status + "，标题：" + title + "。",
                    "success", ticketId, ticketNo, null, false, null);
        }
        return new ToolOutcome("query_ticket", text(output, "message", "已查询你的最近工单。"),
                "success", null, null, null, false, null);
    }

    private Map<String, Object> parseObservation(String observation) {
        if (observation == null || observation.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(observation, new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of("answer", observation);
        }
    }

    private String normalizeStatus(String runtimeStatus) {
        return "COMPLETED".equalsIgnoreCase(runtimeStatus) ? "success" : "failed";
    }

    private String text(Map<String, Object> map, String key) {
        return text(map, key, null);
    }

    private String text(Map<String, Object> map, String key, String fallback) {
        Object value = map != null ? map.get(key) : null;
        return value != null && !String.valueOf(value).isBlank() ? String.valueOf(value) : fallback;
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value != null && !String.valueOf(value).isBlank()) {
            try {
                return Long.parseLong(String.valueOf(value));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private record ToolOutcome(
            String intent,
            String answer,
            String status,
            Long ticketId,
            String ticketNo,
            Long conversationId,
            boolean approvalRequired,
            ServiceTicketResult pendingTicket
    ) {
        static ToolOutcome empty() {
            return new ToolOutcome("knowledge_qa", null, null, null, null, null, false, null);
        }

        ToolOutcome merge(ToolOutcome next) {
            if (next == null) {
                return this;
            }
            // 保留已有有效字段，同时让后执行的工具覆盖意图、答案和工单信息。
            return new ToolOutcome(
                    next.intent != null ? next.intent : intent,
                    next.answer != null ? next.answer : answer,
                    next.status != null ? next.status : status,
                    next.ticketId != null ? next.ticketId : ticketId,
                    next.ticketNo != null ? next.ticketNo : ticketNo,
                    next.conversationId != null ? next.conversationId : conversationId,
                    approvalRequired || next.approvalRequired,
                    next.pendingTicket != null ? next.pendingTicket : pendingTicket
            );
        }
    }
}
