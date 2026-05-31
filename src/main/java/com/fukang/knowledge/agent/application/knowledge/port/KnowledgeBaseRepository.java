package com.fukang.knowledge.agent.application.knowledge.port;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.KnowledgeBaseDO;

public interface KnowledgeBaseRepository {

    void insert(KnowledgeBaseDO knowledgeBase);

    KnowledgeBaseDO findById(Long id);

    IPage<KnowledgeBaseDO> page(long page, long pageSize, String keyword);

    void updateById(KnowledgeBaseDO knowledgeBase);

    void deleteById(Long id);
}
