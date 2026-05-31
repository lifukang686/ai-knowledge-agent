package com.fukang.knowledge.agent.application.knowledge.port;

import com.fukang.knowledge.agent.infrastructure.persistence.entity.DocumentChunkDO;

import java.util.List;

public interface DocumentChunkRepository {

    List<Long> findIdsByDocumentId(Long documentId);

    long deleteByDocumentId(Long documentId);

    long deleteByDocumentIds(List<Long> documentIds);

    List<DocumentChunkDO> findByDocumentId(Long documentId);
}
