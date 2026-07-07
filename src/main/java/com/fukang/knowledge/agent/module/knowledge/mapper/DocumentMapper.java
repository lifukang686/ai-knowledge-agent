package com.fukang.knowledge.agent.module.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fukang.knowledge.agent.module.knowledge.model.entity.DocumentEntity;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 文档 Mapper 接口
 * <p>提供 document 表的基础 CRUD 操作，由 MyBatis-Plus 自动实现</p>
 */
public interface DocumentMapper extends BaseMapper<DocumentEntity> {

    default IPage<DocumentEntity> pageByKnowledgeBase(Long knowledgeBaseId, long page, long pageSize) {
        LambdaQueryWrapper<DocumentEntity> wrapper = new LambdaQueryWrapper<>();
        if (knowledgeBaseId != null) {
            wrapper.eq(DocumentEntity::getKnowledgeBaseId, knowledgeBaseId);
        }
        wrapper.orderByDesc(DocumentEntity::getCreateTime);
        return selectPage(new Page<>(page, pageSize), wrapper);
    }

    default long countByKnowledgeBase(Long knowledgeBaseId) {
        return selectCount(new LambdaQueryWrapper<DocumentEntity>().eq(DocumentEntity::getKnowledgeBaseId, knowledgeBaseId));
    }

    default Map<Long, Long> countByKnowledgeBaseIds(List<Long> knowledgeBaseIds) {
        if (knowledgeBaseIds == null || knowledgeBaseIds.isEmpty()) {
            return Map.of();
        }
        return selectList(new LambdaQueryWrapper<DocumentEntity>().in(DocumentEntity::getKnowledgeBaseId, knowledgeBaseIds))
                .stream()
                .collect(Collectors.groupingBy(DocumentEntity::getKnowledgeBaseId, Collectors.counting()));
    }

    default List<DocumentEntity> findByKnowledgeBase(Long knowledgeBaseId) {
        return selectList(new LambdaQueryWrapper<DocumentEntity>().eq(DocumentEntity::getKnowledgeBaseId, knowledgeBaseId));
    }

    default List<DocumentEntity> findByStatus(String status) {
        return selectList(new LambdaQueryWrapper<DocumentEntity>().eq(DocumentEntity::getStatus, status));
    }
}
