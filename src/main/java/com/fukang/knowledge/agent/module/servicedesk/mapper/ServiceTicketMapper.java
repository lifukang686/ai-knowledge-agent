package com.fukang.knowledge.agent.module.servicedesk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fukang.knowledge.agent.common.enums.ServiceTypeEnum;
import com.fukang.knowledge.agent.module.servicedesk.model.entity.ServiceTicketEntity;
import com.fukang.knowledge.agent.common.enums.TicketStatusEnum;

import java.util.List;

/**
 * 服务台工单 Mapper。
 */
public interface ServiceTicketMapper extends BaseMapper<ServiceTicketEntity> {

    /**
     * 按工单号查询当前用户可见的工单。
     */
    default ServiceTicketEntity findByTicketNoAndCreatorId(String ticketNo, Long creatorId) {
        return selectOne(new LambdaQueryWrapper<ServiceTicketEntity>()
                .eq(ServiceTicketEntity::getTicketNo, ticketNo)
                .eq(ServiceTicketEntity::getCreatorId, creatorId));
    }

    /**
     * 分页查询当前用户工单，并按可选状态和服务类型过滤。
     */
    default IPage<ServiceTicketEntity> pageByCreator(Long creatorId, long page, long pageSize,
                                                     TicketStatusEnum status, ServiceTypeEnum serviceType) {
        LambdaQueryWrapper<ServiceTicketEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ServiceTicketEntity::getCreatorId, creatorId);
        if (status != null) {
            wrapper.eq(ServiceTicketEntity::getStatus, status.name());
        }
        if (serviceType != null && serviceType != ServiceTypeEnum.AUTO) {
            wrapper.eq(ServiceTicketEntity::getServiceType, serviceType.name());
        }
        wrapper.orderByDesc(ServiceTicketEntity::getCreateTime);
        return selectPage(new Page<>(page, pageSize), wrapper);
    }

    /**
     * 查询当前用户最近创建的工单。
     */
    default List<ServiceTicketEntity> findRecentByCreator(Long creatorId, int limit) {
        return selectList(new LambdaQueryWrapper<ServiceTicketEntity>()
                .eq(ServiceTicketEntity::getCreatorId, creatorId)
                .orderByDesc(ServiceTicketEntity::getCreateTime)
                .last("LIMIT " + Math.max(1, limit)));
    }
}
