package com.fukang.knowledge.agent.module.knowledge.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fukang.knowledge.agent.module.knowledge.model.dto.CreateKnowledgeBaseReq;
import com.fukang.knowledge.agent.module.knowledge.model.dto.UpdateKnowledgeBaseReq;
import com.fukang.knowledge.agent.module.knowledge.mapper.DocumentMapper;
import com.fukang.knowledge.agent.module.knowledge.mapper.KnowledgeBaseMapper;
import com.fukang.knowledge.agent.module.knowledge.model.vo.KnowledgeBaseResult;
import com.fukang.knowledge.agent.module.evaluation.service.EvaluationService;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.common.result.PageResponse;
import com.fukang.knowledge.agent.module.knowledge.model.entity.DocumentEntity;
import com.fukang.knowledge.agent.module.knowledge.model.entity.KnowledgeBaseEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 知识库应用服务。
 * <p>只负责知识库自身的创建、查询、更新和删除；文档操作已拆到 {@link DocumentService}。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private final DocumentMapper documentMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final DocumentService documentService;
    private final EvaluationService evaluationService;

    /**
     * 创建知识库。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createKnowledgeBase(CreateKnowledgeBaseReq req) {
        KnowledgeBaseEntity kb = new KnowledgeBaseEntity();
        kb.setName(req.getName());
        kb.setDescription(req.getDescription());
        knowledgeBaseMapper.insert(kb);
        log.info("知识库创建成功: id={}, name={}", kb.getId(), req.getName());
        return kb.getId();
    }

    /**
     * 分页查询知识库列表，并附带文档数量统计。
     */
    public PageResponse<KnowledgeBaseResult> listKnowledgeBases(long page, long pageSize, String keyword) {
        IPage<KnowledgeBaseEntity> resultPage = knowledgeBaseMapper.page(page, pageSize, keyword);

        List<KnowledgeBaseEntity> records = resultPage.getRecords();
        Map<Long, Long> docCountMap = loadDocumentCounts(records);

        List<KnowledgeBaseResult> items = records.stream()
                .map(kb -> toKnowledgeBaseResult(kb, docCountMap.getOrDefault(kb.getId(), 0L)))
                .collect(Collectors.toList());

        return new PageResponse<>(items, resultPage.getTotal(), resultPage.getCurrent(), resultPage.getSize());
    }

    /**
     * 查询单个知识库详情。
     */
    public KnowledgeBaseResult getKnowledgeBase(Long id) {
        KnowledgeBaseEntity kb = findKnowledgeBaseById(id);
        long docCount = documentMapper.countByKnowledgeBase(id);
        return toKnowledgeBaseResult(kb, docCount);
    }

    /**
     * 更新知识库基础信息。
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateKnowledgeBase(Long id, UpdateKnowledgeBaseReq req) {
        KnowledgeBaseEntity kb = findKnowledgeBaseById(id);
        if (StringUtils.hasText(req.getName())) {
            kb.setName(req.getName());
        }
        if (req.getDescription() != null) {
            kb.setDescription(req.getDescription());
        }
        knowledgeBaseMapper.updateById(kb);
        log.info("知识库已更新: id={}, name={}", id, kb.getName());
    }

    /**
     * 删除知识库。
     * <p>先清理文档、向量、文件和评测数据，再删除知识库主记录。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteKnowledgeBase(Long id) {
        KnowledgeBaseEntity kb = findKnowledgeBaseById(id);
        // 复用文档删除链路，确保 chunk、向量索引和 MinIO 文件同步清理。
        List<Long> documentIds = documentMapper.findByKnowledgeBase(id).stream()
                .map(DocumentEntity::getId)
                .toList();
        for (Long documentId : documentIds) {
            documentService.deleteDocument(documentId);
        }
        evaluationService.deleteDatasetsByKnowledgeBase(id);
        knowledgeBaseMapper.deleteById(id);
        log.info("知识库已删除: id={}, name={}, documentCount={}", id, kb.getName(), documentIds.size());
    }

    /**
     * 查询知识库，不存在时抛业务异常。
     */
    private KnowledgeBaseEntity findKnowledgeBaseById(Long id) {
        KnowledgeBaseEntity kb = knowledgeBaseMapper.selectById(id);
        if (kb == null) {
            log.warn("知识库不存在: id={}", id);
            throw new BaseException(ErrorCodeEnum.KNOWLEDGE_BASE_NOT_EXIST);
        }
        return kb;
    }

    /**
     * 批量加载知识库文档数量。
     */
    private Map<Long, Long> loadDocumentCounts(List<KnowledgeBaseEntity> knowledgeBases) {
        if (knowledgeBases.isEmpty()) {
            return Map.of();
        }
        List<Long> kbIds = knowledgeBases.stream().map(KnowledgeBaseEntity::getId).toList();
        return documentMapper.countByKnowledgeBaseIds(kbIds);
    }

    /**
     * 转换知识库结果。
     */
    private KnowledgeBaseResult toKnowledgeBaseResult(KnowledgeBaseEntity kb, long documentCount) {
        return new KnowledgeBaseResult(
                kb.getId(),
                kb.getName(),
                kb.getDescription(),
                documentCount,
                "completed",
                kb.getCreateTime(),
                kb.getUpdateTime()
        );
    }
}
