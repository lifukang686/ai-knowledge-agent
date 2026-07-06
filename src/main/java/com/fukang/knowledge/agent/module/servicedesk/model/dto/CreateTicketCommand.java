package com.fukang.knowledge.agent.module.servicedesk.model.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fukang.knowledge.agent.common.enums.ServiceTypeEnum;
import com.fukang.knowledge.agent.common.enums.TicketPriorityEnum;
import com.fukang.knowledge.agent.common.enums.TicketStatusEnum;

/**
 * 创建服务台工单命令。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTicketCommand {
    private ServiceTypeEnum serviceType;

    private String category;

    private TicketPriorityEnum priority;

    private String title;

    private String description;

    private String agentSummary;

    private Long creatorId;

    private Long sourceRunId;

    private Long sourceConversationId;

    private TicketStatusEnum initialStatus;

}
