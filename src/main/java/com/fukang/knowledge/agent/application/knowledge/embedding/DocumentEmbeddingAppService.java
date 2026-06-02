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
import java.util.List;

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

    @Transactional(rollbackFor = Exception.class)
    public ChunkStorageResult embedAndStore(Long documentId, Long knowledgeBaseId) {
        List<DocumentChunkDO> chunks = chunkStorageService.findByDocumentId(documentId);
        return embedAndStoreWithChunks(chunks, knowledgeBaseId);
    }

    @Transactional(rollbackFor = Exception.class)
    public ChunkStorageResult embedAndStoreWithChunks(List<DocumentChunkDO> chunks, Long knowledgeBaseId) {
        validateChunks(chunks, knowledgeBaseId);

        List<String> texts = extractTexts(chunks);
        log.info("开始文档块向量化: chunkCount={}, knowledgeBaseId={}", texts.size(), knowledgeBaseId);

        EmbeddingResult embeddingResult = embeddingService.embed(texts);
        updateEmbeddingMetadata(chunks, embeddingResult);

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
    }

    private List<String> extractTexts(List<DocumentChunkDO> chunks) {
        List<String> texts = new ArrayList<>(chunks.size());
        for (DocumentChunkDO chunk : chunks) {
            String embeddingText = chunk.getEmbeddingText();
            texts.add(embeddingText != null && !embeddingText.isBlank()
                    ? embeddingText
                    : chunk.getChunkText());
        }
        return texts;
    }

    private void updateEmbeddingMetadata(List<DocumentChunkDO> chunks, EmbeddingResult embeddingResult) {
        Long modelId = embeddingResult.modelId();
        Integer dimension = embeddingResult.dimension();
        String version = embeddingResult.modelVersion();

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
