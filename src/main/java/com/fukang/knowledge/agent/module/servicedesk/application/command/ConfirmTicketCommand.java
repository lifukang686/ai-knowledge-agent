package com.fukang.knowledge.agent.module.servicedesk.application.command;

/**
 * 确认草稿工单命令。
 */
public record ConfirmTicketCommand(
        Long ticketId,
        Long userId
) {
}
