package com.fukang.knowledge.agent.application.knowledge.port;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.DocumentChunkStrategyDO;

/**
 * 文档分块策略配置仓储端口。
 */
public interface DocumentChunkStrategyRepository {

    void insert(DocumentChunkStrategyDO strategy);

    DocumentChunkStrategyDO findById(Long id);

    DocumentChunkStrategyDO findDefault();

    IPage<DocumentChunkStrategyDO> page(long page, long pageSize, String keyword);

    void updateById(DocumentChunkStrategyDO strategy);

    void deleteById(Long id);

    void clearDefault();
}
