package com.fukang.knowledge.agent.module.servicedesk.service.agent;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fukang.knowledge.agent.module.servicedesk.service.stream.ServiceDeskStreamHandler;
import com.fukang.knowledge.agent.common.enums.ServiceTypeEnum;

/**
 * 服务台 Agent 工具调用上下文。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceDeskAgentContext {
    private Long userId;

    private Long runId;

    private Long knowledgeBaseId;

    private Long conversationId;

    private ServiceTypeEnum serviceType;

    private String question;

    private ServiceDeskStreamHandler streamHandler;

}
