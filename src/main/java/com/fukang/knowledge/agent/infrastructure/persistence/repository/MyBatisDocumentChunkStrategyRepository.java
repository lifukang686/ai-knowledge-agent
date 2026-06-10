package com.fukang.knowledge.agent.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fukang.knowledge.agent.application.knowledge.port.DocumentChunkStrategyRepository;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.DocumentChunkStrategyDO;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.DocumentChunkStrategyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 文档分块策略配置仓储 MyBatis 实现。
 */
@Repository
@RequiredArgsConstructor
public class MyBatisDocumentChunkStrategyRepository implements DocumentChunkStrategyRepository {

    private final DocumentChunkStrategyMapper chunkStrategyMapper;

    @Override
    public void insert(DocumentChunkStrategyDO strategy) {
        chunkStrategyMapper.insert(strategy);
    }

    @Override
    public DocumentChunkStrategyDO findById(Long id) {
        return chunkStrategyMapper.selectById(id);
    }

    @Override
    public DocumentChunkStrategyDO findDefault() {
        List<DocumentChunkStrategyDO> strategies = chunkStrategyMapper.selectList(
                new LambdaQueryWrapper<DocumentChunkStrategyDO>()
                        .eq(DocumentChunkStrategyDO::getIsDefault, true)
                        .last("limit 1"));
        return strategies.isEmpty() ? null : strategies.get(0);
    }

    @Override
    public IPage<DocumentChunkStrategyDO> page(long page, long pageSize, String keyword) {
        LambdaQueryWrapper<DocumentChunkStrategyDO> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(DocumentChunkStrategyDO::getStrategyName, keyword)
                    .or()
                    .like(DocumentChunkStrategyDO::getChunkType, keyword));
        }
        wrapper.orderByDesc(DocumentChunkStrategyDO::getIsDefault)
                .orderByDesc(DocumentChunkStrategyDO::getCreateTime);
        return chunkStrategyMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    @Override
    public void updateById(DocumentChunkStrategyDO strategy) {
        chunkStrategyMapper.updateById(strategy);
    }

    @Override
    public void deleteById(Long id) {
        chunkStrategyMapper.deleteById(id);
    }

    @Override
    public void clearDefault() {
        DocumentChunkStrategyDO update = new DocumentChunkStrategyDO();
        update.setIsDefault(false);
        chunkStrategyMapper.update(update,
                new LambdaQueryWrapper<DocumentChunkStrategyDO>()
                        .eq(DocumentChunkStrategyDO::getIsDefault, true));
    }
}
