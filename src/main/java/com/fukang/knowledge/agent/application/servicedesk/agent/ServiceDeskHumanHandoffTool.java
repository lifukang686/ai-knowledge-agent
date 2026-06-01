package com.fukang.knowledge.agent.application.servicedesk.agent;

import com.fukang.knowledge.agent.application.servicedesk.TicketAppService;
import com.fukang.knowledge.agent.application.servicedesk.command.CreateTicketCommand;
import com.fukang.knowledge.agent.application.servicedesk.result.ServiceTicketResult;
import com.fukang.knowledge.agent.domain.servicedesk.TicketPriority;
import com.fukang.knowledge.agent.domain.servicedesk.TicketStatus;
import com.fukang.knowledge.agent.infrastructure.tool.LocalMethodTool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 服务台人工介入工具：生成高优先级草稿工单并记录人工介入事件。
 */
@Component
@RequiredArgsConstructor
public class ServiceDeskHumanHandoffTool implements LocalMethodTool {

    private final TicketAppService ticketAppService;

    @Override
    public String name() {
        return ServiceDeskToolNames.REQUEST_HUMAN_HANDOFF;
    }

    @Override
    public Object execute(Map<String, Object> arguments) {
        ServiceDeskAgentContext context = ServiceDeskAgentContextHolder.getRequired();
        String reason = text(arguments, "reason", "Agent 判断需要人工介入");
        ServiceTicketResult ticket = ticketAppService.createTicket(new CreateTicketCommand(
                context.serviceType(),
                text(arguments, "category", "人工介入"),
                TicketPriority.HIGH,
                text(arguments, "title", titleFromQuestion(context.question())),
                context.question(),
                reason,
                context.userId(),
                context.runId(),
                context.conversationId(),
                TicketStatus.DRAFT
        ));
        ticketAppService.recordHandoffRequested(ticket.id(), context.userId(), reason);
        return ServiceDeskDraftTicketTool.toDraftPayload(ticket);
    }

    private String text(Map<String, Object> args, String key, String fallback) {
        Object value = args != null ? args.get(key) : null;
        return value != null && !String.valueOf(value).isBlank() ? String.valueOf(value) : fallback;
    }

    private String titleFromQuestion(String question) {
        String text = question != null ? question.trim().replaceAll("\\s+", " ") : "人工介入请求";
        if (text.isBlank()) {
            return "人工介入请求";
        }
        return text.length() > 60 ? text.substring(0, 60) : text;
    }
}
