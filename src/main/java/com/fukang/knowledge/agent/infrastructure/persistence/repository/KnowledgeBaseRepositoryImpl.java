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
public class KnowledgeBaseRepositoryImpl implements KnowledgeBaseRepository {

    private final KnowledgeBaseMapper knowledgeBaseMapper;

    /**
     * 新增知识库。
     */
    @Override
    public void insert(KnowledgeBaseDO knowledgeBase) {
        knowledgeBaseMapper.insert(knowledgeBase);
    }

    /**
     * 按 ID 查询知识库。
     */
    @Override
    public KnowledgeBaseDO findById(Long id) {
        return knowledgeBaseMapper.selectById(id);
    }

    /**
     * 分页查询知识库。
     */
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

    /**
     * 更新知识库。
     */
    @Override
    public void updateById(KnowledgeBaseDO knowledgeBase) {
        knowledgeBaseMapper.updateById(knowledgeBase);
    }

    /**
     * 删除知识库。
     */
    @Override
    public void deleteById(Long id) {
        knowledgeBaseMapper.deleteById(id);
    }
}
