package com.fukang.knowledge.agent.module.servicedesk.application.command;

import com.fukang.knowledge.agent.module.servicedesk.domain.ServiceType;
import com.fukang.knowledge.agent.module.servicedesk.domain.TicketPriority;
import com.fukang.knowledge.agent.module.servicedesk.domain.TicketStatus;

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
