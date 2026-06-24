package com.fukang.knowledge.agent.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fukang.knowledge.agent.application.agent.port.ToolDefinitionRepository;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.ToolDefinitionDO;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.ToolDefinitionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 工具定义仓储端口的 MyBatis 实现。
 */
@Repository
@RequiredArgsConstructor
public class ToolDefinitionRepositoryImpl implements ToolDefinitionRepository {

    private final ToolDefinitionMapper toolDefinitionMapper;

    @Override
    public List<ToolDefinitionDO> findEnabled() {
        return toolDefinitionMapper.selectList(
                new LambdaQueryWrapper<ToolDefinitionDO>().eq(ToolDefinitionDO::getEnabled, true));
    }

    @Override
    public ToolDefinitionDO findEnabledByName(String name) {
        return toolDefinitionMapper.selectOne(
                new LambdaQueryWrapper<ToolDefinitionDO>()
                        .eq(ToolDefinitionDO::getName, name)
                        .eq(ToolDefinitionDO::getEnabled, true));
    }
}
