package com.fukang.knowledge.agent.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fukang.knowledge.agent.application.knowledge.port.DocumentChunkRepository;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.DocumentChunkDO;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.DocumentChunkMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 文档块仓储端口的 MyBatis 实现。
 */
@Repository
@RequiredArgsConstructor
public class MyBatisDocumentChunkRepository implements DocumentChunkRepository {

    private final DocumentChunkMapper documentChunkMapper;

    /**
     * 查询文档块 ID 列表。
     */
    @Override
    public List<Long> findIdsByDocumentId(Long documentId) {
        return documentChunkMapper.selectList(
                        new LambdaQueryWrapper<DocumentChunkDO>()
                                .select(DocumentChunkDO::getId)
                                .eq(DocumentChunkDO::getDocumentId, documentId))
                .stream()
                .map(DocumentChunkDO::getId)
                .toList();
    }

    /**
     * 删除单文档块。
     */
    @Override
    public long deleteByDocumentId(Long documentId) {
        return documentChunkMapper.delete(
                new LambdaQueryWrapper<DocumentChunkDO>().eq(DocumentChunkDO::getDocumentId, documentId));
    }

    /**
     * 批量删除文档块。
     */
    @Override
    public long deleteByDocumentIds(List<Long> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            return 0;
        }
        return documentChunkMapper.delete(
                new LambdaQueryWrapper<DocumentChunkDO>().in(DocumentChunkDO::getDocumentId, documentIds));
    }

    /**
     * 查询文档块并按块序排序。
     */
    @Override
    public List<DocumentChunkDO> findByDocumentId(Long documentId) {
        return documentChunkMapper.selectList(
                new LambdaQueryWrapper<DocumentChunkDO>()
                        .eq(DocumentChunkDO::getDocumentId, documentId)
                        .orderByAsc(DocumentChunkDO::getChunkOrder));
    }
}
