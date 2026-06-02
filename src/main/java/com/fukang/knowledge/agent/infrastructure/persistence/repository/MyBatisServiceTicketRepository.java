package com.fukang.knowledge.agent.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fukang.knowledge.agent.application.servicedesk.port.ServiceTicketRepository;
import com.fukang.knowledge.agent.domain.servicedesk.ServiceType;
import com.fukang.knowledge.agent.domain.servicedesk.TicketStatus;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.ServiceTicketDO;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.ServiceTicketMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class MyBatisServiceTicketRepository implements ServiceTicketRepository {

    private final ServiceTicketMapper serviceTicketMapper;

    @Override
    public void insert(ServiceTicketDO ticket) {
        serviceTicketMapper.insert(ticket);
    }

    @Override
    public ServiceTicketDO findById(Long ticketId) {
        return serviceTicketMapper.selectById(ticketId);
    }

    @Override
    public ServiceTicketDO findByTicketNoAndCreatorId(String ticketNo, Long creatorId) {
        LambdaQueryWrapper<ServiceTicketDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ServiceTicketDO::getTicketNo, ticketNo)
                .eq(ServiceTicketDO::getCreatorId, creatorId);
        return serviceTicketMapper.selectOne(wrapper);
    }

    @Override
    public IPage<ServiceTicketDO> pageByCreator(Long creatorId, long page, long pageSize,
                                                TicketStatus status, ServiceType serviceType) {
        LambdaQueryWrapper<ServiceTicketDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ServiceTicketDO::getCreatorId, creatorId);
        if (status != null) {
            wrapper.eq(ServiceTicketDO::getStatus, status.name());
        }
        if (serviceType != null && serviceType != ServiceType.AUTO) {
            wrapper.eq(ServiceTicketDO::getServiceType, serviceType.name());
        }
        wrapper.orderByDesc(ServiceTicketDO::getCreateTime);
        return serviceTicketMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    @Override
    public List<ServiceTicketDO> findRecentByCreator(Long creatorId, int limit) {
        LambdaQueryWrapper<ServiceTicketDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ServiceTicketDO::getCreatorId, creatorId)
                .orderByDesc(ServiceTicketDO::getCreateTime)
                .last("LIMIT " + Math.max(1, limit));
        return serviceTicketMapper.selectList(wrapper);
    }

    @Override
    public void updateById(ServiceTicketDO ticket) {
        serviceTicketMapper.updateById(ticket);
    }
}
