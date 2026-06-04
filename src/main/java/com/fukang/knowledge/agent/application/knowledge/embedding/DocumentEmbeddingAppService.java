package com.fukang.knowledge.agent.application.knowledge.embedding;

import com.fukang.knowledge.agent.application.knowledge.port.DocumentRepository;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.domain.knowledge.model.ChunkStorageResult;
import com.fukang.knowledge.agent.domain.knowledge.model.EmbeddingResult;
import com.fukang.knowledge.agent.infrastructure.ai.EmbeddingIndexStorageService;
import com.fukang.knowledge.agent.infrastructure.persistence.DocumentChunkStorageService;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.DocumentChunkDO;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.DocumentDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 文档向量化应用服务。
 * <p>负责读取已持久化的文档块，调用 embedding 模型生成向量，并写入 pgvector 向量索引。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentEmbeddingAppService {

    private final EmbeddingService embeddingService;
    private final EmbeddingIndexStorageService embeddingIndexStorageService;
    private final DocumentChunkStorageService chunkStorageService;
    private final DocumentRepository documentRepository;

    /**
     * 读取指定文档的所有 chunk，并写入向量索引。
     */
    @Transactional(rollbackFor = Exception.class)
    public ChunkStorageResult embedAndStore(Long documentId, Long knowledgeBaseId) {
        List<DocumentChunkDO> chunks = chunkStorageService.findByDocumentId(documentId);
        return embedAndStoreWithChunks(chunks, knowledgeBaseId);
    }

    /**
     * 对已加载的 chunk 执行向量化，适用于管道复用和测试场景。
     */
    @Transactional(rollbackFor = Exception.class)
    public ChunkStorageResult embedAndStoreWithChunks(List<DocumentChunkDO> chunks, Long knowledgeBaseId) {
        // 先校验 chunk 和知识库上下文，避免无效数据进入模型调用。
        validateChunks(chunks, knowledgeBaseId);

        // 取向量化输入文本：优先 embeddingText，缺失时回退 chunkText。
        List<String> texts = extractTexts(chunks);
        log.info("开始文档块向量化: chunkCount={}, knowledgeBaseId={}", texts.size(), knowledgeBaseId);

        // 调用 embedding 模型生成向量，并确认返回结果与 chunk 一一对应。
        EmbeddingResult embeddingResult = embeddingService.embed(texts);
        validateEmbeddingResult(embeddingResult, chunks.size());

        // 回写模型版本、维度等元数据，便于后续判断是否需要重建向量。
        updateEmbeddingMetadata(chunks, embeddingResult);

        // 将向量和原始 chunk 信息写入 pgvector 索引。
        return embeddingIndexStorageService.saveVectorsToPgVector(chunks, embeddingResult, knowledgeBaseId);
    }

    private void validateChunks(List<DocumentChunkDO> chunks, Long knowledgeBaseId) {
        if (chunks == null || chunks.isEmpty()) {
            log.warn("文档块列表为空，无法执行向量化: knowledgeBaseId={}", knowledgeBaseId);
            throw new BaseException(ErrorCodeEnum.CHUNK_DATA_EMPTY);
        }
        if (knowledgeBaseId == null) {
            log.warn("知识库 ID 为空，无法存储向量索引");
            throw new BaseException(ErrorCodeEnum.KNOWLEDGE_BASE_NOT_EXIST);
        }
        for (DocumentChunkDO chunk : chunks) {
            String text = selectEmbeddingInput(chunk);
            if (text == null || text.isBlank()) {
                log.warn("文档块缺少可向量化文本: documentId={}, chunkId={}, chunkOrder={}",
                        chunk.getDocumentId(), chunk.getId(), chunk.getChunkOrder());
                throw new BaseException(ErrorCodeEnum.CHUNK_VALIDATION_FAILED);
            }
        }
    }

    private List<String> extractTexts(List<DocumentChunkDO> chunks) {
        List<String> texts = new ArrayList<>(chunks.size());
        for (DocumentChunkDO chunk : chunks) {
            texts.add(selectEmbeddingInput(chunk));
        }
        return texts;
    }

    private String selectEmbeddingInput(DocumentChunkDO chunk) {
        String embeddingText = chunk.getEmbeddingText();
        // embeddingText 会补标题和位置上下文；缺失时退回原始 chunkText 保证入库不中断。
        return embeddingText != null && !embeddingText.isBlank()
                ? embeddingText
                : chunk.getChunkText();
    }

    private void validateEmbeddingResult(EmbeddingResult embeddingResult, int expectedChunks) {
        if (embeddingResult == null
                || embeddingResult.embeddings() == null
                || embeddingResult.embeddings().size() != expectedChunks
                || !embeddingResult.allSucceeded()) {
            log.warn("向量化结果不完整: expectedChunks={}, actualChunks={}, allSucceeded={}",
                    expectedChunks,
                    embeddingResult != null && embeddingResult.embeddings() != null
                            ? embeddingResult.embeddings().size()
                            : 0,
                    embeddingResult != null && embeddingResult.allSucceeded());
            throw new BaseException(ErrorCodeEnum.EMBEDDING_FAILED);
        }

        Set<Integer> seenOrders = new HashSet<>();
        for (EmbeddingResult.EmbeddingVector vector : embeddingResult.embeddings()) {
            int chunkOrder = vector.chunkOrder();
            if (chunkOrder < 0 || chunkOrder >= expectedChunks || !seenOrders.add(chunkOrder)) {
                log.warn("向量化结果 chunkOrder 非法: chunkOrder={}, expectedChunks={}", chunkOrder, expectedChunks);
                throw new BaseException(ErrorCodeEnum.EMBEDDING_FAILED);
            }
        }
    }

    private void updateEmbeddingMetadata(List<DocumentChunkDO> chunks, EmbeddingResult embeddingResult) {
        Long modelId = embeddingResult.modelId();
        Integer dimension = embeddingResult.dimension();
        String version = embeddingResult.modelVersion();

        // chunk 与 document 同步记录模型元数据，便于后续判断是否需要重建向量。
        for (DocumentChunkDO chunk : chunks) {
            chunk.setEmbeddingModelId(modelId);
            chunk.setEmbeddingDimension(dimension);
            chunk.setEmbeddingVersion(version);
            chunkStorageService.updateById(chunk);
        }

        Long documentId = chunks.get(0).getDocumentId();
        DocumentDO document = documentRepository.findById(documentId);
        if (document != null) {
            document.setEmbeddingModelId(modelId);
            document.setEmbeddingDimension(dimension);
            document.setEmbeddingVersion(version);
            documentRepository.updateById(document);
        }
    }
}
