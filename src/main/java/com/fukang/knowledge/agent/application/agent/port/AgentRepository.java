package com.fukang.knowledge.agent.application.agent.port;

import com.fukang.knowledge.agent.infrastructure.persistence.entity.AgentDO;

/**
 * Agent 配置仓储端口。
 * <p>应用层只依赖该端口，具体 MyBatis 查询由基础设施层适配。</p>
 */
public interface AgentRepository {

    void insert(AgentDO agent);

    AgentDO findById(Long id);
}
