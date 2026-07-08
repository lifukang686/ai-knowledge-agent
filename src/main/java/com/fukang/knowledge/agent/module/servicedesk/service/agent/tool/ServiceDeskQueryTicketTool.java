package com.fukang.knowledge.agent.module.servicedesk.service.agent.tool;

import com.fukang.knowledge.agent.module.servicedesk.service.TicketService;
import com.fukang.knowledge.agent.module.servicedesk.model.vo.ServiceTicketResult;
import com.fukang.knowledge.agent.module.agent.service.tool.LocalMethodTool;
import com.fukang.knowledge.agent.module.servicedesk.model.bo.ServiceDeskAgentContext;
import com.fukang.knowledge.agent.module.servicedesk.service.agent.ServiceDeskAgentContextHolder;
import com.fukang.knowledge.agent.module.servicedesk.service.agent.ServiceDeskToolNames;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 服务台工单查询工具：只能查询当前用户自己的工单。
 */
@Component
@RequiredArgsConstructor
public class ServiceDeskQueryTicketTool implements LocalMethodTool {

    private final TicketService ticketService;

    /**
     * 返回工具名称。
     */
    @Override
    public String name() {
        return ServiceDeskToolNames.QUERY_TICKET;
    }

    /**
     * 查询当前用户工单。
     */
    @Override
    public Object execute(Map<String, Object> arguments) {
        ServiceDeskAgentContext context = ServiceDeskAgentContextHolder.getRequired();
        String ticketNo = text(arguments, "ticketNo", "");
        if (!ticketNo.isBlank()) {
            // 按当前用户过滤工单号，避免查询到他人数据。
            ServiceTicketResult ticket = ticketService.getTicketByNo(ticketNo, context.getUserId());
            return ticket != null
                    ? Map.of("found", true, "ticket", toTicketPayload(ticket))
                    : Map.of("found", false, "message", "没有找到工单 " + ticketNo);
        }
        // 未指定工单号时，仅返回当前用户最近工单。
        List<Map<String, Object>> tickets = ticketService.listRecentTickets(context.getUserId(), 5)
                .stream()
                .map(this::toTicketPayload)
                .toList();
        return Map.of("found", !tickets.isEmpty(), "tickets", tickets);
    }

    /**
     * 转换工单查询载荷。
     */
    private Map<String, Object> toTicketPayload(ServiceTicketResult ticket) {
        return Map.of(
                "ticketId", ticket.getId(),
                "ticketNo", ticket.getTicketNo(),
                "status", ticket.getStatus(),
                "title", ticket.getTitle(),
                "serviceType", ticket.getServiceType(),
                "priority", ticket.getPriority(),
                "category", ticket.getCategory() != null ? ticket.getCategory() : ""
        );
    }

    /**
     * 读取文本参数。
     */
    private String text(Map<String, Object> args, String key, String fallback) {
        Object value = args != null ? args.get(key) : null;
        return value != null ? String.valueOf(value).trim() : fallback;
    }
}
