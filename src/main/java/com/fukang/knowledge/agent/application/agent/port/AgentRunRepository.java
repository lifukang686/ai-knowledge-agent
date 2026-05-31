package com.fukang.knowledge.agent.application.agent.port;

import com.fukang.knowledge.agent.infrastructure.persistence.entity.AgentRunDO;

/**
 * Agent 运行记录仓储端口。
 */
public interface AgentRunRepository {

    void insert(AgentRunDO run);

    AgentRunDO findById(Long id);

    void updateById(AgentRunDO run);
}
