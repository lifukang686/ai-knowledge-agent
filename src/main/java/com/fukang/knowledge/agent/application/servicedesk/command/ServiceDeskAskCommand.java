package com.fukang.knowledge.agent.application.servicedesk.command;

/**
 * 服务台问答命令。
 */
public record ServiceDeskAskCommand(
        /**
         * 问题
         */
        String question,
        /**
         * 业务类型(自动识别、IT、HR)
         */
        String serviceType,
        /**
         * 知识库ID
         */
        Long knowledgeBaseId,
        /**
         * 会话ID
         */
        Long conversationId
) {
}
