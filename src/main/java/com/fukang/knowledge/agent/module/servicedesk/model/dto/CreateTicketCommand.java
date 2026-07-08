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
    /**
     * 工单所属服务类型。
     */
    private ServiceTypeEnum serviceType;

    /**
     * 工单分类。
     */
    private String category;

    /**
     * 工单优先级。
     */
    private TicketPriorityEnum priority;

    /**
     * 工单标题。
     */
    private String title;

    /**
     * 工单问题描述。
     */
    private String description;

    /**
     * Agent 对问题和处理建议的摘要。
     */
    private String agentSummary;

    /**
     * 工单创建人用户 ID。
     */
    private Long creatorId;

    /**
     * 来源服务台运行记录 ID。
     */
    private Long sourceRunId;

    /**
     * 来源会话 ID。
     */
    private Long sourceConversationId;

    /**
     * 工单初始状态。
     */
    private TicketStatusEnum initialStatus;

}
