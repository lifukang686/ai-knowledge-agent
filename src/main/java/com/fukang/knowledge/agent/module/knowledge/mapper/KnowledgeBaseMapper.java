package com.fukang.knowledge.agent.module.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fukang.knowledge.agent.module.knowledge.model.entity.KnowledgeBaseEntity;
import org.springframework.util.StringUtils;

/**
 * 知识库 Mapper 接口
 * <p>提供 knowledge_base 表的基础 CRUD 操作，由 MyBatis-Plus 自动实现</p>
 */
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBaseEntity> {

    default IPage<KnowledgeBaseEntity> page(long page, long pageSize, String keyword) {
        LambdaQueryWrapper<KnowledgeBaseEntity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(KnowledgeBaseEntity::getName, keyword)
                    .or()
                    .like(KnowledgeBaseEntity::getDescription, keyword));
        }
        wrapper.orderByDesc(KnowledgeBaseEntity::getCreateTime);
        return selectPage(new Page<>(page, pageSize), wrapper);
    }
}
