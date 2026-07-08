package com.fukang.knowledge.agent.module.servicedesk.model.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fukang.knowledge.agent.common.enums.ServiceTypeEnum;

/**
 * 服务台问答命令。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceDeskAskCommand {
    /**
     * 用户提交的原始问题。
     */
    private String question;

    /**
     * 解析或指定后的服务类型编码。
     */
    private String serviceType;

    /**
     * 本次问答使用的知识库 ID。
     */
    private Long knowledgeBaseId;

    /**
     * 本次问答关联的会话 ID。
     */
    private Long conversationId;

    public ServiceDeskAskCommand withServiceType(ServiceTypeEnum resolvedServiceType) {
        return new ServiceDeskAskCommand(
                question,
                resolvedServiceType != null ? resolvedServiceType.name() : serviceType,
                knowledgeBaseId,
                conversationId);
    }

}
