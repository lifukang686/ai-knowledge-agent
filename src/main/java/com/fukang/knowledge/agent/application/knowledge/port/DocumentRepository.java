package com.fukang.knowledge.agent.application.knowledge.port;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.DocumentDO;

import java.util.List;
import java.util.Map;

/**
 * 文档仓储端口，供应用层操作文档元数据。
 */
public interface DocumentRepository {

    void insert(DocumentDO document);

    DocumentDO findById(Long id);

    IPage<DocumentDO> pageByKnowledgeBase(Long knowledgeBaseId, long page, long pageSize);

    void updateById(DocumentDO document);

    long countByKnowledgeBase(Long knowledgeBaseId);

    Map<Long, Long> countByKnowledgeBaseIds(List<Long> knowledgeBaseIds);

    List<DocumentDO> findByKnowledgeBase(Long knowledgeBaseId);

    /** 按处理状态查询文档，供启动补偿扫描使用。 */
    List<DocumentDO> findByStatus(String status);

    void deleteById(Long id);

    void deleteByKnowledgeBase(Long knowledgeBaseId);
}
