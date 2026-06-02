package com.fukang.knowledge.agent.infrastructure.persistence.repository;

import com.fukang.knowledge.agent.application.agent.port.AgentRunRepository;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.AgentRunDO;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.AgentRunMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * Agent 运行记录仓储端口的 MyBatis 实现。
 */
@Repository
@RequiredArgsConstructor
public class MyBatisAgentRunRepository implements AgentRunRepository {

    private final AgentRunMapper agentRunMapper;

    @Override
    public void insert(AgentRunDO run) {
        agentRunMapper.insert(run);
    }

    @Override
    public AgentRunDO findById(Long id) {
        return agentRunMapper.selectById(id);
    }

    @Override
    public void updateById(AgentRunDO run) {
        agentRunMapper.updateById(run);
    }
}
