package com.fukang.knowledge.agent.module.servicedesk.service.agent.tool;

import com.fukang.knowledge.agent.module.servicedesk.service.TicketService;
import com.fukang.knowledge.agent.module.servicedesk.model.dto.CreateTicketCommand;
import com.fukang.knowledge.agent.module.servicedesk.model.vo.ServiceTicketResult;
import com.fukang.knowledge.agent.common.enums.TicketPriorityEnum;
import com.fukang.knowledge.agent.common.enums.TicketStatusEnum;
import com.fukang.knowledge.agent.module.agent.service.tool.LocalMethodTool;
import com.fukang.knowledge.agent.module.servicedesk.service.agent.ServiceDeskAgentContext;
import com.fukang.knowledge.agent.module.servicedesk.service.agent.ServiceDeskAgentContextHolder;
import com.fukang.knowledge.agent.module.servicedesk.service.agent.ServiceDeskToolNames;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 服务台人工介入工具：生成高优先级草稿工单并记录人工介入事件。
 */
@Component
@RequiredArgsConstructor
public class ServiceDeskHumanHandoffTool implements LocalMethodTool {

    private final TicketService ticketService;

    /**
     * 返回工具名称。
     */
    @Override
    public String name() {
        return ServiceDeskToolNames.REQUEST_HUMAN_HANDOFF;
    }

    /**
     * 生成人工介入草稿工单。
     */
    @Override
    public Object execute(Map<String, Object> arguments) {
        ServiceDeskAgentContext context = ServiceDeskAgentContextHolder.getRequired();
        String reason = text(arguments, "reason", "Agent 判断需要人工介入");
        // 人工介入也只生成高优先级草稿，避免 Agent 直接提交。
        ServiceTicketResult ticket = ticketService.createTicket(new CreateTicketCommand(
                context.getServiceType(),
                text(arguments, "category", "人工介入"),
                TicketPriorityEnum.HIGH,
                text(arguments, "title", titleFromQuestion(context.getQuestion())),
                context.getQuestion(),
                reason,
                context.getUserId(),
                context.getRunId(),
                context.getConversationId(),
                TicketStatusEnum.DRAFT
        ));
        // 记录人工介入事件，便于工单轨迹追踪。
        ticketService.recordHandoffRequested(ticket.getId(), context.getUserId(), reason);
        return ServiceDeskDraftTicketTool.toDraftPayload(ticket);
    }

    /**
     * 读取文本参数。
     */
    private String text(Map<String, Object> args, String key, String fallback) {
        Object value = args != null ? args.get(key) : null;
        return value != null && !String.valueOf(value).isBlank() ? String.valueOf(value) : fallback;
    }

    /**
     * 从问题生成工单标题。
     */
    private String titleFromQuestion(String question) {
        String text = question != null ? question.trim().replaceAll("\\s+", " ") : "人工介入请求";
        if (text.isBlank()) {
            return "人工介入请求";
        }
        return text.length() > 60 ? text.substring(0, 60) : text;
    }
}
