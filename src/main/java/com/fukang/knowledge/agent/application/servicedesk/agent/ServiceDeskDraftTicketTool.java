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
 * 服务台工单草稿工具：写操作只创建 DRAFT，等待用户确认。
 */
@Component
@RequiredArgsConstructor
public class ServiceDeskDraftTicketTool implements LocalMethodTool {

    private final TicketAppService ticketAppService;

    @Override
    public String name() {
        return ServiceDeskToolNames.DRAFT_TICKET;
    }

    @Override
    public Object execute(Map<String, Object> arguments) {
        ServiceDeskAgentContext context = ServiceDeskAgentContextHolder.getRequired();
        ServiceTicketResult ticket = ticketAppService.createTicket(new CreateTicketCommand(
                context.serviceType(),
                text(arguments, "category", "综合"),
                TicketPriority.from(text(arguments, "priority", "MEDIUM")),
                text(arguments, "title", titleFromQuestion(context.question())),
                context.question(),
                text(arguments, "summary", context.question()),
                context.userId(),
                context.runId(),
                context.conversationId(),
                TicketStatus.DRAFT
        ));
        return toDraftPayload(ticket);
    }

    static Map<String, Object> toDraftPayload(ServiceTicketResult ticket) {
        return Map.of(
                "approvalRequired", true,
                "ticketId", ticket.id(),
                "ticketNo", ticket.ticketNo(),
                "status", ticket.status(),
                "serviceType", ticket.serviceType(),
                "category", ticket.category() != null ? ticket.category() : "",
                "priority", ticket.priority(),
                "title", ticket.title(),
                "summary", ticket.agentSummary() != null ? ticket.agentSummary() : ""
        );
    }

    private String text(Map<String, Object> args, String key, String fallback) {
        Object value = args != null ? args.get(key) : null;
        return value != null && !String.valueOf(value).isBlank() ? String.valueOf(value) : fallback;
    }

    private String titleFromQuestion(String question) {
        String text = question != null ? question.trim().replaceAll("\\s+", " ") : "服务台请求";
        if (text.isBlank()) {
            return "服务台请求";
        }
        return text.length() > 60 ? text.substring(0, 60) : text;
    }
}
