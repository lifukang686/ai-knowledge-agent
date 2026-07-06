package com.fukang.knowledge.agent.module.servicedesk.model.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fukang.knowledge.agent.module.agent.model.vo.AgentRunEvent;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 服务台运行记录结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceDeskRunResult {
    private Long id;

    private Long userId;

    private String question;

    private String serviceType;

    private String intent;

    private Long knowledgeBaseId;

    private Long conversationId;

    private String answer;

    private String status;

    private Long ticketId;

    private Boolean approvalRequired;

    private Long pendingTicketId;

    private Long feedbackId;

    private List<AgentRunEvent> events;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private LocalDateTime createTime;

}
