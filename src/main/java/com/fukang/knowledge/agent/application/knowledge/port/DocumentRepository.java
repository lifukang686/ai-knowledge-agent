package com.fukang.knowledge.agent.application.knowledge.port;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.DocumentDO;

import java.util.List;
import java.util.Map;

public interface DocumentRepository {

    void insert(DocumentDO document);

    DocumentDO findById(Long id);

    IPage<DocumentDO> pageByKnowledgeBase(Long knowledgeBaseId, long page, long pageSize);

    void updateById(DocumentDO document);

    long countByKnowledgeBase(Long knowledgeBaseId);

    Map<Long, Long> countByKnowledgeBaseIds(List<Long> knowledgeBaseIds);

    List<DocumentDO> findByKnowledgeBase(Long knowledgeBaseId);

    List<DocumentDO> findByStatus(String status);

    void deleteById(Long id);

    void deleteByKnowledgeBase(Long knowledgeBaseId);
}
