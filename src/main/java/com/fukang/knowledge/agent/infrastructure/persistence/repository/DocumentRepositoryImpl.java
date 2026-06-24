package com.fukang.knowledge.agent.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fukang.knowledge.agent.application.knowledge.port.DocumentRepository;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.DocumentDO;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.DocumentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 文档仓储端口的 MyBatis 实现。
 */
@Repository
@RequiredArgsConstructor
public class DocumentRepositoryImpl implements DocumentRepository {

    private final DocumentMapper documentMapper;

    /**
     * 新增文档。
     */
    @Override
    public void insert(DocumentDO document) {
        documentMapper.insert(document);
    }

    /**
     * 按 ID 查询文档。
     */
    @Override
    public DocumentDO findById(Long id) {
        return documentMapper.selectById(id);
    }

    /**
     * 按知识库分页查询文档。
     */
    @Override
    public IPage<DocumentDO> pageByKnowledgeBase(Long knowledgeBaseId, long page, long pageSize) {
        LambdaQueryWrapper<DocumentDO> wrapper = new LambdaQueryWrapper<>();
        if (knowledgeBaseId != null) {
            wrapper.eq(DocumentDO::getKnowledgeBaseId, knowledgeBaseId);
        }
        wrapper.orderByDesc(DocumentDO::getCreateTime);
        return documentMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    /**
     * 更新文档。
     */
    @Override
    public void updateById(DocumentDO document) {
        documentMapper.updateById(document);
    }

    /**
     * 统计知识库文档数。
     */
    @Override
    public long countByKnowledgeBase(Long knowledgeBaseId) {
        return documentMapper.selectCount(
                new LambdaQueryWrapper<DocumentDO>().eq(DocumentDO::getKnowledgeBaseId, knowledgeBaseId));
    }

    /**
     * 批量统计知识库文档数。
     */
    @Override
    public Map<Long, Long> countByKnowledgeBaseIds(List<Long> knowledgeBaseIds) {
        if (knowledgeBaseIds == null || knowledgeBaseIds.isEmpty()) {
            return Map.of();
        }
        // 批量统计文档数量，供知识库列表一次性补齐统计字段。
        List<DocumentDO> docs = documentMapper.selectList(
                new LambdaQueryWrapper<DocumentDO>().in(DocumentDO::getKnowledgeBaseId, knowledgeBaseIds));
        return docs.stream()
                .collect(Collectors.groupingBy(DocumentDO::getKnowledgeBaseId, Collectors.counting()));
    }

    /**
     * 查询知识库下全部文档。
     */
    @Override
    public List<DocumentDO> findByKnowledgeBase(Long knowledgeBaseId) {
        return documentMapper.selectList(
                new LambdaQueryWrapper<DocumentDO>().eq(DocumentDO::getKnowledgeBaseId, knowledgeBaseId));
    }

    /**
     * 按处理状态查询文档。
     */
    @Override
    public List<DocumentDO> findByStatus(String status) {
        return documentMapper.selectList(
                new LambdaQueryWrapper<DocumentDO>().eq(DocumentDO::getStatus, status));
    }

    /**
     * 删除单个文档。
     */
    @Override
    public void deleteById(Long id) {
        documentMapper.deleteById(id);
    }

    /**
     * 删除知识库下文档。
     */
    @Override
    public void deleteByKnowledgeBase(Long knowledgeBaseId) {
        documentMapper.delete(new LambdaQueryWrapper<DocumentDO>().eq(DocumentDO::getKnowledgeBaseId, knowledgeBaseId));
    }
}
