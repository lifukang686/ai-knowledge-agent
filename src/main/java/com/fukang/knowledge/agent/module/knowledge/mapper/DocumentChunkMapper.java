package com.fukang.knowledge.agent.module.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fukang.knowledge.agent.module.knowledge.model.entity.DocumentChunkEntity;

import java.util.List;

/**
 * 文档块 Mapper 接口
 * <p>提供 document_chunk 表的基础 CRUD 操作，由 MyBatis-Plus 自动实现</p>
 */
public interface DocumentChunkMapper extends BaseMapper<DocumentChunkEntity> {

    default List<Long> findIdsByDocumentId(Long documentId) {
        return selectList(new LambdaQueryWrapper<DocumentChunkEntity>()
                        .select(DocumentChunkEntity::getId)
                        .eq(DocumentChunkEntity::getDocumentId, documentId))
                .stream()
                .map(DocumentChunkEntity::getId)
                .toList();
    }

    default long deleteByDocumentId(Long documentId) {
        return delete(new LambdaQueryWrapper<DocumentChunkEntity>().eq(DocumentChunkEntity::getDocumentId, documentId));
    }

    default long deleteByDocumentIds(List<Long> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            return 0;
        }
        return delete(new LambdaQueryWrapper<DocumentChunkEntity>().in(DocumentChunkEntity::getDocumentId, documentIds));
    }

    default List<DocumentChunkEntity> findByDocumentId(Long documentId) {
        return selectList(new LambdaQueryWrapper<DocumentChunkEntity>()
                .eq(DocumentChunkEntity::getDocumentId, documentId)
                .orderByAsc(DocumentChunkEntity::getChunkOrder));
    }
}
