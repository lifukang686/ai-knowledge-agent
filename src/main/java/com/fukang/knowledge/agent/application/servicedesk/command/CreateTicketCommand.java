package com.fukang.knowledge.agent.application.servicedesk.command;

import com.fukang.knowledge.agent.domain.servicedesk.ServiceType;
import com.fukang.knowledge.agent.domain.servicedesk.TicketPriority;
import com.fukang.knowledge.agent.domain.servicedesk.TicketStatus;

/**
 * 创建服务台工单命令。
 */
public record CreateTicketCommand(
        ServiceType serviceType,
        String category,
        TicketPriority priority,
        String title,
        String description,
        String agentSummary,
        Long creatorId,
        Long sourceRunId,
        Long sourceConversationId,
        TicketStatus initialStatus
) {
}
