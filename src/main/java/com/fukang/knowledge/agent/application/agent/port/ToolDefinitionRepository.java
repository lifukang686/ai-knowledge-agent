package com.fukang.knowledge.agent.application.agent.port;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.ToolDefinitionDO;

import java.util.List;

/**
 * 工具定义仓储端口。
 */
public interface ToolDefinitionRepository {

    List<ToolDefinitionDO> findEnabled();

    IPage<ToolDefinitionDO> page(long current, long size, String keyword);

    ToolDefinitionDO findEnabledByName(String name);

    ToolDefinitionDO findById(Long id);

    List<ToolDefinitionDO> findByIds(List<Long> ids);

    void insert(ToolDefinitionDO tool);

    void updateById(ToolDefinitionDO tool);

    void deleteById(Long id);

    void deleteByName(String name);
}
