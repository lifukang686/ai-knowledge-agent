package com.fukang.knowledge.agent.application.servicedesk.port;

import com.fukang.knowledge.agent.infrastructure.persistence.entity.ServiceDeskRunDO;

/**
 * 服务台运行记录仓储端口。
 */
public interface ServiceDeskRunRepository {

    void insert(ServiceDeskRunDO run);

    ServiceDeskRunDO findById(Long runId);

    void updateById(ServiceDeskRunDO run);
}
