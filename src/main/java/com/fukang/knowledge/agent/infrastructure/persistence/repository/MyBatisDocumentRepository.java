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
public class MyBatisDocumentRepository implements DocumentRepository {

    private final DocumentMapper documentMapper;

    @Override
    public void insert(DocumentDO document) {
        documentMapper.insert(document);
    }

    @Override
    public DocumentDO findById(Long id) {
        return documentMapper.selectById(id);
    }

    @Override
    public IPage<DocumentDO> pageByKnowledgeBase(Long knowledgeBaseId, long page, long pageSize) {
        LambdaQueryWrapper<DocumentDO> wrapper = new LambdaQueryWrapper<>();
        if (knowledgeBaseId != null) {
            wrapper.eq(DocumentDO::getKnowledgeBaseId, knowledgeBaseId);
        }
        wrapper.orderByDesc(DocumentDO::getCreateTime);
        return documentMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    @Override
    public void updateById(DocumentDO document) {
        documentMapper.updateById(document);
    }

    @Override
    public long countByKnowledgeBase(Long knowledgeBaseId) {
        return documentMapper.selectCount(
                new LambdaQueryWrapper<DocumentDO>().eq(DocumentDO::getKnowledgeBaseId, knowledgeBaseId));
    }

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

    @Override
    public List<DocumentDO> findByKnowledgeBase(Long knowledgeBaseId) {
        return documentMapper.selectList(
                new LambdaQueryWrapper<DocumentDO>().eq(DocumentDO::getKnowledgeBaseId, knowledgeBaseId));
    }

    @Override
    public List<DocumentDO> findByStatus(String status) {
        return documentMapper.selectList(
                new LambdaQueryWrapper<DocumentDO>().eq(DocumentDO::getStatus, status));
    }

    @Override
    public void deleteById(Long id) {
        documentMapper.deleteById(id);
    }

    @Override
    public void deleteByKnowledgeBase(Long knowledgeBaseId) {
        documentMapper.delete(new LambdaQueryWrapper<DocumentDO>().eq(DocumentDO::getKnowledgeBaseId, knowledgeBaseId));
    }
}
