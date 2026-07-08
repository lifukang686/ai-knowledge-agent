package com.fukang.knowledge.agent.module.servicedesk.model.bo;

import com.fukang.knowledge.agent.common.enums.ServiceTypeEnum;
import com.fukang.knowledge.agent.module.servicedesk.service.stream.ServiceDeskStreamHandler;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 服务台 Agent 工具调用上下文。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceDeskAgentContext {
    /**
     * 当前用户 ID。
     */
    private Long userId;

    /**
     * 当前服务台运行记录 ID。
     */
    private Long runId;

    /**
     * 当前使用的知识库 ID。
     */
    private Long knowledgeBaseId;

    /**
     * 当前关联的会话 ID。
     */
    private Long conversationId;

    /**
     * 当前服务类型。
     */
    private ServiceTypeEnum serviceType;

    /**
     * 用户原始问题。
     */
    private String question;

    /**
     * 服务台流式输出处理器。
     */
    private ServiceDeskStreamHandler streamHandler;

}
