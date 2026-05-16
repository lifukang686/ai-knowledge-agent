package com.fukang.knowledge.agent.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.EmbeddingIndexDO;

/**
 * 嵌入向量索引 Mapper 接口
 * <p>提供 embedding_index 表的基础 CRUD 操作，由 MyBatis-Plus 自动实现</p>
 */
public interface EmbeddingIndexMapper extends BaseMapper<EmbeddingIndexDO> {
}