package com.fukang.knowledge.agent.module.servicedesk.application;

import com.fukang.knowledge.agent.module.servicedesk.domain.ServiceDeskIntent;
import com.fukang.knowledge.agent.module.servicedesk.domain.ServiceType;
import com.fukang.knowledge.agent.module.servicedesk.domain.TicketPriority;

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
