package com.fukang.knowledge.agent.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fukang.knowledge.agent.application.knowledge.port.KnowledgeBaseRepository;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.KnowledgeBaseDO;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.KnowledgeBaseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

/**
 * 知识库仓储端口的 MyBatis 实现。
 */
@Repository
@RequiredArgsConstructor
public class MyBatisKnowledgeBaseRepository implements KnowledgeBaseRepository {

    private final KnowledgeBaseMapper knowledgeBaseMapper;

    @Override
    public void insert(KnowledgeBaseDO knowledgeBase) {
        knowledgeBaseMapper.insert(knowledgeBase);
    }

    @Override
    public KnowledgeBaseDO findById(Long id) {
        return knowledgeBaseMapper.selectById(id);
    }

    @Override
    public IPage<KnowledgeBaseDO> page(long page, long pageSize, String keyword) {
        LambdaQueryWrapper<KnowledgeBaseDO> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(KnowledgeBaseDO::getName, keyword)
                    .or()
                    .like(KnowledgeBaseDO::getDescription, keyword));
        }
        wrapper.orderByDesc(KnowledgeBaseDO::getCreateTime);
        return knowledgeBaseMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    @Override
    public void updateById(KnowledgeBaseDO knowledgeBase) {
        knowledgeBaseMapper.updateById(knowledgeBase);
    }

    @Override
    public void deleteById(Long id) {
        knowledgeBaseMapper.deleteById(id);
    }
}
