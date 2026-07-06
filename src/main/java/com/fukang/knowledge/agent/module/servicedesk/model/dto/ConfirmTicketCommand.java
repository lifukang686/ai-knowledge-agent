package com.fukang.knowledge.agent.module.servicedesk.model.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 确认草稿工单命令。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmTicketCommand {
    private Long ticketId;

    private Long userId;

}
