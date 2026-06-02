package com.fukang.knowledge.agent.application.servicedesk.port;

import com.fukang.knowledge.agent.infrastructure.persistence.entity.ServiceTicketEventDO;

import java.util.List;

/**
 * 服务台工单事件仓储端口。
 */
public interface ServiceTicketEventRepository {

    void insert(ServiceTicketEventDO event);

    List<ServiceTicketEventDO> findByTicketId(Long ticketId);

    Long countByTicketId(Long ticketId);
}
