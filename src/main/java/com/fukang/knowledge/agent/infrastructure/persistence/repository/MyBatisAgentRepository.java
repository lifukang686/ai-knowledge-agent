package com.fukang.knowledge.agent.infrastructure.persistence.repository;

import com.fukang.knowledge.agent.application.agent.port.AgentRepository;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.AgentDO;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.AgentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * Agent 仓储端口的 MyBatis 实现。
 */
@Repository
@RequiredArgsConstructor
public class MyBatisAgentRepository implements AgentRepository {

    private final AgentMapper agentMapper;

    @Override
    public void insert(AgentDO agent) {
        agentMapper.insert(agent);
    }

    @Override
    public AgentDO findById(Long id) {
        return agentMapper.selectById(id);
    }
}
