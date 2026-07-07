package com.fukang.knowledge.agent.module.servicedesk.service.agent;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fukang.knowledge.agent.module.agent.service.runtime.AgentRuntimeOptions;
import com.fukang.knowledge.agent.module.agent.service.runtime.PlanExecuteAgentRuntime;
import com.fukang.knowledge.agent.module.agent.service.tool.impl.ScopedToolRegistry;
import com.fukang.knowledge.agent.module.servicedesk.service.stream.ServiceDeskStreamHandler;
import com.fukang.knowledge.agent.module.servicedesk.model.dto.ServiceDeskAskCommand;
import com.fukang.knowledge.agent.module.servicedesk.model.vo.ServiceDeskAnswerResult;
import com.fukang.knowledge.agent.module.servicedesk.model.vo.ServiceTicketResult;
import com.fukang.knowledge.agent.module.agent.model.vo.AgentRunEvent;
import com.fukang.knowledge.agent.module.agent.model.vo.AgentStep;
import com.fukang.knowledge.agent.common.enums.ServiceTypeEnum;
import com.fukang.knowledge.agent.common.enums.TicketStatusEnum;
import com.fukang.knowledge.agent.module.modelruntime.service.manager.PromptTemplateManager;
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

    /**
     * 最大执行步数。
     */
    private static final int MAX_STEPS = 4;

    /**
     * 规划提示词模板。
     */
    private static final String PLANNING_PROMPT = "service-desk/agent-planning.v1";

    /**
     * 通用 Plan-Execute 运行时。
     */
    private final PlanExecuteAgentRuntime planExecuteAgentRuntime;

    /**
     * 服务台工具工厂。
     */
    private final ServiceDeskAgentToolFactory toolFactory;

    /**
     * 提示词模板管理器。
     */
    private final PromptTemplateManager promptTemplateManager;

    /**
     * JSON 序列化工具。
     */
    private final ObjectMapper objectMapper;

    /**
     * 在服务台受控工具集合内执行一次用户问题处理。
     */
    public ServiceDeskAnswerResult run(ServiceDeskAskCommand command, Long userId, Long runId,
                                       ServiceDeskStreamHandler handler) {
        ServiceTypeEnum serviceType = ServiceTypeEnum.from(command.getServiceType());
        // 将本次服务台请求放入上下文，LocalMethodTool 执行时按线程读取。
        ServiceDeskAgentContext context = new ServiceDeskAgentContext(
                userId, runId, command.getKnowledgeBaseId(), command.getConversationId(),
                serviceType, command.getQuestion(), handler);
        // 只暴露服务台内置工具，限制 Agent 的可调用范围。
        ScopedToolRegistry toolScope = toolFactory.createScope();
        String planningPrompt = promptTemplateManager.renderText(PLANNING_PROMPT, Map.of(
                "serviceType", serviceType.name(),
                "knowledgeBaseId", command.getKnowledgeBaseId() != null ? String.valueOf(command.getKnowledgeBaseId()) : "",
                "conversationId", command.getConversationId() != null ? String.valueOf(command.getConversationId()) : ""
        ));

        try {
            ServiceDeskAgentContextHolder.set(context);
            AgentRuntimeOptions options = AgentRuntimeOptions.of(MAX_STEPS, planningPrompt, toolScope)
                    .withEventListener(handler != null ? handler::onAgentEvent : null);
            // 交给通用 Plan-Execute Runtime 规划并执行工具链。
            PlanExecuteAgentRuntime.RuntimeResult result = planExecuteAgentRuntime.runTask(command.getQuestion(), options);
            return toAnswerResult(result, runId, serviceType.name());
        } finally {
            ServiceDeskAgentContextHolder.clear();
        }
    }

    /**
     * 转换服务台回答结果。
     */
    private ServiceDeskAnswerResult toAnswerResult(PlanExecuteAgentRuntime.RuntimeResult result,
                                                   Long runId, String serviceType) {
        // 从执行步骤中提取业务结果，覆盖模型可能不准确的最终话术。
        ToolOutcome outcome = extractOutcome(result.getSteps());
        String answer = result.getAnswer();
        if (outcome.isApprovalRequired()) {
            // 写操作只能生成草稿，最终话术以工具事实为准，防止模型误说“已正式创建”。
            answer = outcome.getAnswer();
        } else if (answer == null || answer.isBlank()) {
            answer = outcome.getAnswer() != null ? outcome.getAnswer() : "服务台 Agent 已完成处理。";
        }
        String status = normalizeStatus(result.getStatus());
        if (!"failed".equals(status) && outcome.getStatus() != null) {
            status = outcome.getStatus();
        }
        return new ServiceDeskAnswerResult(
                answer,
                outcome.getIntent(),
                serviceType,
                status,
                runId,
                outcome.getTicketId(),
                outcome.getTicketNo(),
                outcome.getConversationId(),
                outcome.isApprovalRequired(),
                outcome.getPendingTicket(),
                result.getEvents(),
                false
        );
    }

    /**
     * 提取工具执行结果。
     */
    private ToolOutcome extractOutcome(List<AgentStep> steps) {
        ToolOutcome outcome = ToolOutcome.empty();
        for (AgentStep step : steps) {
            // observation 是工具 JSON 输出，解析后再按工具名转成服务台结果。
            Map<String, Object> output = parseObservation(step.getObservation());
            // 多个工具结果按执行顺序合并，后续写操作结果优先覆盖普通问答结果。
            outcome = outcome.merge(toolOutcome(step.getToolName(), output));
        }
        return outcome;
    }

    /**
     * 转换单个工具结果。
     */
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

    /**
     * 转换工单类结果。
     */
    private ToolOutcome ticketOutcome(String intent, Map<String, Object> output) {
        Long ticketId = longValue(output.get("ticketId"));
        String ticketNo = text(output, "ticketNo");
        // 工单写操作返回待确认草稿，前端据此展示确认入口。
        ServiceTicketResult pendingTicket = new ServiceTicketResult(
                ticketId,
                ticketNo,
                text(output, "serviceType"),
                text(output, "category"),
                text(output, "priority"),
                text(output, "status", TicketStatusEnum.DRAFT.name()),
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

    /**
     * 转换工单查询结果。
     */
    private ToolOutcome queryOutcome(Map<String, Object> output) {
        Object ticket = output.get("ticket");
        if (ticket instanceof Map<?, ?> map) {
            // 命中单个工单时，压缩成用户可读的状态摘要。
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

    /**
     * 解析观察结果。
     */
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

    /**
     * 标准化运行状态。
     */
    private String normalizeStatus(String runtimeStatus) {
        return "COMPLETED".equalsIgnoreCase(runtimeStatus) ? "success" : "failed";
    }

    /**
     * 读取文本字段。
     */
    private String text(Map<String, Object> map, String key) {
        return text(map, key, null);
    }

    /**
     * 读取文本字段，支持默认值。
     */
    private String text(Map<String, Object> map, String key, String fallback) {
        Object value = map != null ? map.get(key) : null;
        return value != null && !String.valueOf(value).isBlank() ? String.valueOf(value) : fallback;
    }

    /**
     * 转换 Long 值。
     */
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

    /**
     * 工具执行汇总结果。
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class ToolOutcome {
        private String intent;

        private String answer;

        private String status;

        private Long ticketId;

        private String ticketNo;

        private Long conversationId;

        private boolean approvalRequired;

        private ServiceTicketResult pendingTicket;

        /**
         * 创建空结果。
         */
        static ToolOutcome empty() {
            return new ToolOutcome("knowledge_qa", null, null, null, null, null, false, null);
        }

        /**
         * 合并后续结果。
         */
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
