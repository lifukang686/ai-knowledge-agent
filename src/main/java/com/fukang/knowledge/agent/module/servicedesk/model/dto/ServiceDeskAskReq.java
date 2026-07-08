package com.fukang.knowledge.agent.module.servicedesk.model.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 服务台问答请求。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceDeskAskReq {
    /**
     * 用户提交的服务台问题。
     */
    private String question;

    /**
     * 服务类型，允许前端指定 IT、HR 等业务域。
     */
    private String serviceType;

    /**
     * 用于回答问题的知识库 ID。
     */
    private Long knowledgeBaseId;

    /**
     * 关联的会话 ID，为空时表示新会话或无会话上下文。
     */
    private Long conversationId;

}
