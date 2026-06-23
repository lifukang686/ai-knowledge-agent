package com.fukang.knowledge.agent.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fukang.knowledge.agent.application.servicedesk.port.ServiceTicketEventRepository;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.ServiceTicketEventDO;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.ServiceTicketEventMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 服务台工单事件仓储端口的 MyBatis 实现。
 */
@Repository
@RequiredArgsConstructor
public class MyBatisServiceTicketEventRepository implements ServiceTicketEventRepository {

    private final ServiceTicketEventMapper serviceTicketEventMapper;

    /**
     * 新增工单事件。
     */
    @Override
    public void insert(ServiceTicketEventDO event) {
        serviceTicketEventMapper.insert(event);
    }

    /**
     * 查询工单事件列表。
     */
    @Override
    public List<ServiceTicketEventDO> findByTicketId(Long ticketId) {
        LambdaQueryWrapper<ServiceTicketEventDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ServiceTicketEventDO::getTicketId, ticketId)
                .orderByAsc(ServiceTicketEventDO::getCreateTime);
        return serviceTicketEventMapper.selectList(wrapper);
    }

    /**
     * 统计工单事件数量。
     */
    @Override
    public Long countByTicketId(Long ticketId) {
        LambdaQueryWrapper<ServiceTicketEventDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ServiceTicketEventDO::getTicketId, ticketId);
        return serviceTicketEventMapper.selectCount(wrapper);
    }
}
