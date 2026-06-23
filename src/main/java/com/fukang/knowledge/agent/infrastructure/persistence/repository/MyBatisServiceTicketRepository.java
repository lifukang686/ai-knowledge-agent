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

/**
 * 服务台工单仓储端口的 MyBatis 实现。
 */
@Repository
@RequiredArgsConstructor
public class MyBatisServiceTicketRepository implements ServiceTicketRepository {

    private final ServiceTicketMapper serviceTicketMapper;

    /**
     * 新增工单。
     */
    @Override
    public void insert(ServiceTicketDO ticket) {
        serviceTicketMapper.insert(ticket);
    }

    /**
     * 按 ID 查询工单。
     */
    @Override
    public ServiceTicketDO findById(Long ticketId) {
        return serviceTicketMapper.selectById(ticketId);
    }

    /**
     * 按工单号和创建人查询工单。
     */
    @Override
    public ServiceTicketDO findByTicketNoAndCreatorId(String ticketNo, Long creatorId) {
        LambdaQueryWrapper<ServiceTicketDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ServiceTicketDO::getTicketNo, ticketNo)
                .eq(ServiceTicketDO::getCreatorId, creatorId);
        return serviceTicketMapper.selectOne(wrapper);
    }

    /**
     * 分页查询当前用户工单。
     */
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

    /**
     * 查询当前用户最近工单。
     */
    @Override
    public List<ServiceTicketDO> findRecentByCreator(Long creatorId, int limit) {
        LambdaQueryWrapper<ServiceTicketDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ServiceTicketDO::getCreatorId, creatorId)
                .orderByDesc(ServiceTicketDO::getCreateTime)
                // limit 来自应用层参数，先做下限保护再拼接到 SQL 尾部。
                .last("LIMIT " + Math.max(1, limit));
        return serviceTicketMapper.selectList(wrapper);
    }

    /**
     * 更新工单。
     */
    @Override
    public void updateById(ServiceTicketDO ticket) {
        serviceTicketMapper.updateById(ticket);
    }
}
