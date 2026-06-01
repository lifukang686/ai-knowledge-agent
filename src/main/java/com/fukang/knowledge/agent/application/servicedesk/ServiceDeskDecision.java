package com.fukang.knowledge.agent.application.servicedesk;

import com.fukang.knowledge.agent.domain.servicedesk.ServiceDeskIntent;
import com.fukang.knowledge.agent.domain.servicedesk.ServiceType;
import com.fukang.knowledge.agent.domain.servicedesk.TicketPriority;

/**
 * 服务台意图识别结果。
 */
public record ServiceDeskDecision(
        ServiceDeskIntent intent,
        ServiceType serviceType,
        String category,
        TicketPriority priority,
        String title,
        String summary,
        String reason
) {
}
