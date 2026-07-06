package com.fukang.knowledge.agent.module.servicedesk.service;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fukang.knowledge.agent.common.enums.ServiceDeskIntentEnum;
import com.fukang.knowledge.agent.common.enums.ServiceTypeEnum;
import com.fukang.knowledge.agent.common.enums.TicketPriorityEnum;

/**
 * 服务台意图识别结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceDeskDecision {
    private ServiceDeskIntentEnum intent;

    private ServiceTypeEnum serviceType;

    private String category;

    private TicketPriorityEnum priority;

    private String title;

    private String summary;

    private String reason;

}
