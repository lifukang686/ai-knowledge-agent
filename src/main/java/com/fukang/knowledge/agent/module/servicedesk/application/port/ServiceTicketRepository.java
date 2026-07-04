package com.fukang.knowledge.agent.module.servicedesk.application.port;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fukang.knowledge.agent.module.servicedesk.domain.ServiceType;
import com.fukang.knowledge.agent.module.servicedesk.domain.TicketStatus;
import com.fukang.knowledge.agent.module.servicedesk.infrastructure.persistence.entity.ServiceTicketDO;

import java.util.List;

/**
 * 服务台工单仓储端口。
 */
public interface ServiceTicketRepository {

    void insert(ServiceTicketDO ticket);

    ServiceTicketDO findById(Long ticketId);

    ServiceTicketDO findByTicketNoAndCreatorId(String ticketNo, Long creatorId);

    IPage<ServiceTicketDO> pageByCreator(Long creatorId, long page, long pageSize,
                                         TicketStatus status, ServiceType serviceType);

    List<ServiceTicketDO> findRecentByCreator(Long creatorId, int limit);

    void updateById(ServiceTicketDO ticket);
}
