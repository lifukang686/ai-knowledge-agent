package com.fukang.knowledge.agent.application.knowledge.port;

import com.fukang.knowledge.agent.infrastructure.persistence.entity.DocumentChunkDO;

import java.util.List;

/**
 * 文档块仓储端口，屏蔽 MyBatis 细节。
 */
public interface DocumentChunkRepository {

    /** 查询文档关联的 chunk ID，用于删除向量索引。 */
    List<Long> findIdsByDocumentId(Long documentId);

    long deleteByDocumentId(Long documentId);

    long deleteByDocumentIds(List<Long> documentIds);

    /** 按文档查询已解析入库的 chunk。 */
    List<DocumentChunkDO> findByDocumentId(Long documentId);
}
