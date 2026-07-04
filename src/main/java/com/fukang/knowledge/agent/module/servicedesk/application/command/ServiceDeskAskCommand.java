package com.fukang.knowledge.agent.module.servicedesk.application.command;

import com.fukang.knowledge.agent.module.servicedesk.domain.ServiceType;

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
