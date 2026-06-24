package com.fukang.knowledge.agent.application.agent.port;

import com.fukang.knowledge.agent.infrastructure.persistence.entity.ToolDefinitionDO;

import java.util.List;

/**
 * 工具定义仓储端口。
 */
public interface ToolDefinitionRepository {

    List<ToolDefinitionDO> findEnabled();

    ToolDefinitionDO findEnabledByName(String name);
}
