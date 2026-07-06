package com.fukang.knowledge.agent.module.servicedesk.service.agent.impl;

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
 * 服务台工单草稿工具：写操作只创建 DRAFT，等待用户确认。
 */
@Component
@RequiredArgsConstructor
public class ServiceDeskDraftTicketTool implements LocalMethodTool {

    private final TicketService ticketService;

    /**
     * 返回工具名称。
     */
    @Override
    public String name() {
        return ServiceDeskToolNames.DRAFT_TICKET;
    }

    /**
     * 创建工单草稿。
     */
    @Override
    public Object execute(Map<String, Object> arguments) {
        ServiceDeskAgentContext context = ServiceDeskAgentContextHolder.getRequired();
        // 写操作统一创建草稿，必须由用户确认后才转为正式工单。
        ServiceTicketResult ticket = ticketService.createTicket(new CreateTicketCommand(
                context.getServiceType(),
                text(arguments, "category", "综合"),
                TicketPriorityEnum.from(text(arguments, "priority", "MEDIUM")),
                text(arguments, "title", titleFromQuestion(context.getQuestion())),
                context.getQuestion(),
                text(arguments, "summary", context.getQuestion()),
                context.getUserId(),
                context.getRunId(),
                context.getConversationId(),
                TicketStatusEnum.DRAFT
        ));
        return toDraftPayload(ticket);
    }

    /**
     * 转换草稿工单载荷。
     */
    static Map<String, Object> toDraftPayload(ServiceTicketResult ticket) {
        // 返回给 Runtime 的结构化结果，决定最终话术和前端确认态。
        return Map.of(
                "approvalRequired", true,
                "ticketId", ticket.getId(),
                "ticketNo", ticket.getTicketNo(),
                "status", ticket.getStatus(),
                "serviceType", ticket.getServiceType(),
                "category", ticket.getCategory() != null ? ticket.getCategory() : "",
                "priority", ticket.getPriority(),
                "title", ticket.getTitle(),
                "summary", ticket.getAgentSummary() != null ? ticket.getAgentSummary() : ""
        );
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
        String text = question != null ? question.trim().replaceAll("\\s+", " ") : "服务台请求";
        if (text.isBlank()) {
            return "服务台请求";
        }
        return text.length() > 60 ? text.substring(0, 60) : text;
    }
}
