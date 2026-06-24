package com.fukang.knowledge.agent.application.knowledge;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fukang.knowledge.agent.application.knowledge.command.CreateKnowledgeBaseCommand;
import com.fukang.knowledge.agent.application.knowledge.command.UpdateKnowledgeBaseCommand;
import com.fukang.knowledge.agent.application.knowledge.port.DocumentRepository;
import com.fukang.knowledge.agent.application.knowledge.port.KnowledgeBaseRepository;
import com.fukang.knowledge.agent.application.knowledge.result.KnowledgeBaseResult;
import com.fukang.knowledge.agent.application.evaluation.EvaluationAppService;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.common.result.PageResponse;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.DocumentDO;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.KnowledgeBaseDO;
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
 * <p>只负责知识库自身的创建、查询、更新和删除；文档操作已拆到 {@link DocumentAppService}。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseAppService {

    private final DocumentRepository documentRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final DocumentAppService documentAppService;
    private final EvaluationAppService evaluationAppService;

    /**
     * 创建知识库。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createKnowledgeBase(CreateKnowledgeBaseCommand command) {
        KnowledgeBaseDO kb = new KnowledgeBaseDO();
        kb.setName(command.name());
        kb.setDescription(command.description());
        knowledgeBaseRepository.insert(kb);
        log.info("知识库创建成功: id={}, name={}", kb.getId(), command.name());
        return kb.getId();
    }

    /**
     * 分页查询知识库列表，并附带文档数量统计。
     */
    public PageResponse<KnowledgeBaseResult> listKnowledgeBases(long page, long pageSize, String keyword) {
        IPage<KnowledgeBaseDO> resultPage = knowledgeBaseRepository.page(page, pageSize, keyword);

        List<KnowledgeBaseDO> records = resultPage.getRecords();
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
        KnowledgeBaseDO kb = findKnowledgeBaseById(id);
        long docCount = documentRepository.countByKnowledgeBase(id);
        return toKnowledgeBaseResult(kb, docCount);
    }

    /**
     * 更新知识库基础信息。
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateKnowledgeBase(Long id, UpdateKnowledgeBaseCommand command) {
        KnowledgeBaseDO kb = findKnowledgeBaseById(id);
        if (StringUtils.hasText(command.name())) {
            kb.setName(command.name());
        }
        if (command.description() != null) {
            kb.setDescription(command.description());
        }
        knowledgeBaseRepository.updateById(kb);
        log.info("知识库已更新: id={}, name={}", id, kb.getName());
    }

    /**
     * 删除知识库。
     * <p>先清理文档、向量、文件和评测数据，再删除知识库主记录。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteKnowledgeBase(Long id) {
        KnowledgeBaseDO kb = findKnowledgeBaseById(id);
        // 复用文档删除链路，确保 chunk、向量索引和 MinIO 文件同步清理。
        List<Long> documentIds = documentRepository.findByKnowledgeBase(id).stream()
                .map(DocumentDO::getId)
                .toList();
        for (Long documentId : documentIds) {
            documentAppService.deleteDocument(documentId);
        }
        evaluationAppService.deleteDatasetsByKnowledgeBase(id);
        knowledgeBaseRepository.deleteById(id);
        log.info("知识库已删除: id={}, name={}, documentCount={}", id, kb.getName(), documentIds.size());
    }

    /**
     * 查询知识库，不存在时抛业务异常。
     */
    private KnowledgeBaseDO findKnowledgeBaseById(Long id) {
        KnowledgeBaseDO kb = knowledgeBaseRepository.findById(id);
        if (kb == null) {
            log.warn("知识库不存在: id={}", id);
            throw new BaseException(ErrorCodeEnum.KNOWLEDGE_BASE_NOT_EXIST);
        }
        return kb;
    }

    /**
     * 批量加载知识库文档数量。
     */
    private Map<Long, Long> loadDocumentCounts(List<KnowledgeBaseDO> knowledgeBases) {
        if (knowledgeBases.isEmpty()) {
            return Map.of();
        }
        List<Long> kbIds = knowledgeBases.stream().map(KnowledgeBaseDO::getId).toList();
        return documentRepository.countByKnowledgeBaseIds(kbIds);
    }

    /**
     * 转换知识库结果。
     */
    private KnowledgeBaseResult toKnowledgeBaseResult(KnowledgeBaseDO kb, long documentCount) {
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
