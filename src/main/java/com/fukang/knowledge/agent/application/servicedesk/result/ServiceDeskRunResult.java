package com.fukang.knowledge.agent.application.servicedesk.result;

import com.fukang.knowledge.agent.domain.agent.model.AgentRunEvent;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 服务台运行记录结果。
 */
public record ServiceDeskRunResult(
        Long id,
        Long userId,
        String question,
        String serviceType,
        String intent,
        Long knowledgeBaseId,
        Long conversationId,
        String answer,
        String status,
        Long ticketId,
        Boolean approvalRequired,
        Long pendingTicketId,
        Long feedbackId,
        List<AgentRunEvent> events,
        LocalDateTime startTime,
        LocalDateTime endTime,
        LocalDateTime createTime
) {
}
