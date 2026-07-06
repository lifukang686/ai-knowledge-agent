package com.fukang.knowledge.agent.module.servicedesk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fukang.knowledge.agent.module.servicedesk.model.entity.ServiceDeskFeedbackEntity;

/**
 * 服务台反馈 Mapper。
 */
public interface ServiceDeskFeedbackMapper extends BaseMapper<ServiceDeskFeedbackEntity> {

    default ServiceDeskFeedbackEntity findByRunIdAndUserId(Long runId, Long userId) {
        return selectOne(new LambdaQueryWrapper<ServiceDeskFeedbackEntity>()
                .eq(ServiceDeskFeedbackEntity::getRunId, runId)
                .eq(ServiceDeskFeedbackEntity::getUserId, userId));
    }
}
