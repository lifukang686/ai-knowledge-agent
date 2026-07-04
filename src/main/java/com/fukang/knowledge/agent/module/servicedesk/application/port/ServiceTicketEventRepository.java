package com.fukang.knowledge.agent.module.servicedesk.application.port;

import com.fukang.knowledge.agent.module.servicedesk.infrastructure.persistence.entity.ServiceTicketEventDO;

import java.util.List;

/**
 * 服务台工单事件仓储端口。
 */
public interface ServiceTicketEventRepository {

    void insert(ServiceTicketEventDO event);

    List<ServiceTicketEventDO> findByTicketId(Long ticketId);

    Long countByTicketId(Long ticketId);
}
