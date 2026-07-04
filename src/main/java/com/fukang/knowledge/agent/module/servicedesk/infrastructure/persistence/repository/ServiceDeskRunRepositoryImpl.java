package com.fukang.knowledge.agent.module.servicedesk.infrastructure.persistence.repository;

import com.fukang.knowledge.agent.module.servicedesk.application.port.ServiceDeskRunRepository;
import com.fukang.knowledge.agent.module.servicedesk.infrastructure.persistence.entity.ServiceDeskRunDO;
import com.fukang.knowledge.agent.module.servicedesk.infrastructure.persistence.mapper.ServiceDeskRunMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 服务台运行记录仓储端口的 MyBatis 实现。
 */
@Repository
@RequiredArgsConstructor
public class ServiceDeskRunRepositoryImpl implements ServiceDeskRunRepository {

    private final ServiceDeskRunMapper serviceDeskRunMapper;

    @Override
    public void insert(ServiceDeskRunDO run) {
        serviceDeskRunMapper.insert(run);
    }

    @Override
    public ServiceDeskRunDO findById(Long runId) {
        return serviceDeskRunMapper.selectById(runId);
    }

    @Override
    public void updateById(ServiceDeskRunDO run) {
        serviceDeskRunMapper.updateById(run);
    }
}
