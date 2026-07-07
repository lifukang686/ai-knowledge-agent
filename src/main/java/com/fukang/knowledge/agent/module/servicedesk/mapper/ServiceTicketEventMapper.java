package com.fukang.knowledge.agent.module.servicedesk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fukang.knowledge.agent.module.servicedesk.model.entity.ServiceTicketEventEntity;

import java.util.List;

/**
 * 服务台工单事件 Mapper。
 */
public interface ServiceTicketEventMapper extends BaseMapper<ServiceTicketEventEntity> {

    /**
     * 按时间顺序查询工单事件。
     */
    default List<ServiceTicketEventEntity> findByTicketId(Long ticketId) {
        return selectList(new LambdaQueryWrapper<ServiceTicketEventEntity>()
                .eq(ServiceTicketEventEntity::getTicketId, ticketId)
                .orderByAsc(ServiceTicketEventEntity::getCreateTime));
    }

    /**
     * 统计工单事件数量。
     */
    default Long countByTicketId(Long ticketId) {
        return selectCount(new LambdaQueryWrapper<ServiceTicketEventEntity>()
                .eq(ServiceTicketEventEntity::getTicketId, ticketId));
    }
}
