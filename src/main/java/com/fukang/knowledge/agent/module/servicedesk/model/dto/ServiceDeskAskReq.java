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
    private String question;

    private String serviceType;

    private Long knowledgeBaseId;

    private Long conversationId;

}
