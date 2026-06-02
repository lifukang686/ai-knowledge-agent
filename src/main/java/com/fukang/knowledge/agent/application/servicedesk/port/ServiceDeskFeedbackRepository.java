package com.fukang.knowledge.agent.application.servicedesk.port;

import com.fukang.knowledge.agent.infrastructure.persistence.entity.ServiceDeskFeedbackDO;

/**
 * 服务台反馈仓储端口。
 */
public interface ServiceDeskFeedbackRepository {

    void insert(ServiceDeskFeedbackDO feedback);

    ServiceDeskFeedbackDO findByRunIdAndUserId(Long runId, Long userId);
}
