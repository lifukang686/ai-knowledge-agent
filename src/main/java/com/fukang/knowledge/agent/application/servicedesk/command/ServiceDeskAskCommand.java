package com.fukang.knowledge.agent.application.servicedesk.command;

import com.fukang.knowledge.agent.domain.servicedesk.ServiceType;

/**
 * 服务台问答命令。
 */
public record ServiceDeskAskCommand(
        /**
         * 问题
         */
        String question,
        /**
         * 业务类型（自动识别、IT、HR）
         */
        String serviceType,
        /**
         * 知识库 ID
         */
        Long knowledgeBaseId,
        /**
         * 会话 ID
         */
        Long conversationId
) {
    public ServiceDeskAskCommand withServiceType(ServiceType resolvedServiceType) {
        return new ServiceDeskAskCommand(
                question,
                resolvedServiceType != null ? resolvedServiceType.name() : serviceType,
                knowledgeBaseId,
                conversationId);
    }
}
