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
    private String question;

    private String serviceType;

    private Long knowledgeBaseId;

    private Long conversationId;

    public ServiceDeskAskCommand withServiceType(ServiceTypeEnum resolvedServiceType) {
        return new ServiceDeskAskCommand(
                question,
                resolvedServiceType != null ? resolvedServiceType.name() : serviceType,
                knowledgeBaseId,
                conversationId);
    }

}
