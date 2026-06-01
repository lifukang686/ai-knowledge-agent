package com.fukang.knowledge.agent.application.servicedesk.command;

/**
 * 服务台问答命令。
 */
public record ServiceDeskAskCommand(
        String question,
        String serviceType,
        Long knowledgeBaseId,
        Long conversationId
) {
}
