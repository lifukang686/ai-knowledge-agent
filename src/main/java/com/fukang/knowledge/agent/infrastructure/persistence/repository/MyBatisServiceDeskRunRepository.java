package com.fukang.knowledge.agent.infrastructure.persistence.repository;

import com.fukang.knowledge.agent.application.servicedesk.port.ServiceDeskRunRepository;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.ServiceDeskRunDO;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.ServiceDeskRunMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MyBatisServiceDeskRunRepository implements ServiceDeskRunRepository {

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
