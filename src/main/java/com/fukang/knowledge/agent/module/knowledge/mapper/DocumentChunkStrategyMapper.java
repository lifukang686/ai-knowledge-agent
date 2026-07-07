package com.fukang.knowledge.agent.module.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fukang.knowledge.agent.common.result.PageResponse;
import com.fukang.knowledge.agent.module.knowledge.model.entity.DocumentChunkStrategyEntity;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 文档分块策略 Mapper。
 */
public interface DocumentChunkStrategyMapper extends BaseMapper<DocumentChunkStrategyEntity> {

    default DocumentChunkStrategyEntity findDefault() {
        List<DocumentChunkStrategyEntity> strategies = selectList(new LambdaQueryWrapper<DocumentChunkStrategyEntity>()
                .eq(DocumentChunkStrategyEntity::getIsDefault, true)
                .last("LIMIT 1"));
        return strategies.isEmpty() ? null : strategies.get(0);
    }

    default PageResponse<DocumentChunkStrategyEntity> page(long page, long pageSize, String keyword) {
        LambdaQueryWrapper<DocumentChunkStrategyEntity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(DocumentChunkStrategyEntity::getStrategyName, keyword)
                    .or()
                    .like(DocumentChunkStrategyEntity::getChunkType, keyword));
        }
        wrapper.orderByDesc(DocumentChunkStrategyEntity::getIsDefault)
                .orderByDesc(DocumentChunkStrategyEntity::getCreateTime);
        IPage<DocumentChunkStrategyEntity> resultPage = selectPage(new Page<>(page, pageSize), wrapper);
        return new PageResponse<>(
                resultPage.getRecords(),
                resultPage.getTotal(),
                resultPage.getCurrent(),
                resultPage.getSize());
    }

    default void clearDefault() {
        DocumentChunkStrategyEntity update = new DocumentChunkStrategyEntity();
        update.setIsDefault(false);
        update(update, new LambdaQueryWrapper<DocumentChunkStrategyEntity>()
                .eq(DocumentChunkStrategyEntity::getIsDefault, true));
    }
}
