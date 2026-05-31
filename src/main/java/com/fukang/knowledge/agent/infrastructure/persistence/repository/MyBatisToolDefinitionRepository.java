package com.fukang.knowledge.agent.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fukang.knowledge.agent.application.agent.port.ToolDefinitionRepository;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.ToolDefinitionDO;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.ToolDefinitionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class MyBatisToolDefinitionRepository implements ToolDefinitionRepository {

    private final ToolDefinitionMapper toolDefinitionMapper;

    @Override
    public List<ToolDefinitionDO> findEnabled() {
        return toolDefinitionMapper.selectList(
                new LambdaQueryWrapper<ToolDefinitionDO>().eq(ToolDefinitionDO::getEnabled, true));
    }

    @Override
    public IPage<ToolDefinitionDO> page(long current, long size, String keyword) {
        LambdaQueryWrapper<ToolDefinitionDO> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(ToolDefinitionDO::getName, keyword)
                    .or()
                    .like(ToolDefinitionDO::getDescription, keyword));
        }
        wrapper.orderByDesc(ToolDefinitionDO::getCreateTime);
        return toolDefinitionMapper.selectPage(new Page<>(current, size), wrapper);
    }

    @Override
    public ToolDefinitionDO findEnabledByName(String name) {
        return toolDefinitionMapper.selectOne(
                new LambdaQueryWrapper<ToolDefinitionDO>()
                        .eq(ToolDefinitionDO::getName, name)
                        .eq(ToolDefinitionDO::getEnabled, true));
    }

    @Override
    public ToolDefinitionDO findById(Long id) {
        return toolDefinitionMapper.selectById(id);
    }

    @Override
    public List<ToolDefinitionDO> findByIds(List<Long> ids) {
        return toolDefinitionMapper.selectBatchIds(ids);
    }

    @Override
    public void insert(ToolDefinitionDO tool) {
        toolDefinitionMapper.insert(tool);
    }

    @Override
    public void updateById(ToolDefinitionDO tool) {
        toolDefinitionMapper.updateById(tool);
    }

    @Override
    public void deleteById(Long id) {
        toolDefinitionMapper.deleteById(id);
    }

    @Override
    public void deleteByName(String name) {
        toolDefinitionMapper.delete(
                new LambdaQueryWrapper<ToolDefinitionDO>().eq(ToolDefinitionDO::getName, name));
    }
}
