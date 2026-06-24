package com.fukang.knowledge.agent.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fukang.knowledge.agent.application.servicedesk.port.ServiceDeskFeedbackRepository;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.ServiceDeskFeedbackDO;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.ServiceDeskFeedbackMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 服务台反馈仓储端口的 MyBatis 实现。
 */
@Repository
@RequiredArgsConstructor
public class ServiceDeskFeedbackRepositoryImpl implements ServiceDeskFeedbackRepository {

    private final ServiceDeskFeedbackMapper serviceDeskFeedbackMapper;

    @Override
    public void insert(ServiceDeskFeedbackDO feedback) {
        serviceDeskFeedbackMapper.insert(feedback);
    }

    @Override
    public ServiceDeskFeedbackDO findByRunIdAndUserId(Long runId, Long userId) {
        LambdaQueryWrapper<ServiceDeskFeedbackDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ServiceDeskFeedbackDO::getRunId, runId)
                .eq(ServiceDeskFeedbackDO::getUserId, userId);
        return serviceDeskFeedbackMapper.selectOne(wrapper);
    }
}
