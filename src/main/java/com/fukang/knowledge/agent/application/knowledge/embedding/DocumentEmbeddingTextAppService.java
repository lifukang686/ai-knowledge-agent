package com.fukang.knowledge.agent.application.knowledge.embedding;

import com.fukang.knowledge.agent.application.knowledge.port.DocumentRepository;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.infrastructure.persistence.DocumentChunkStorageService;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.DocumentChunkDO;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.DocumentDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 文档块 embedding 文本构造服务。
 * <p>负责在原始 chunk_text 之外生成专用于向量化的 embedding_text，
 * 让向量输入可以携带标题和片段位置，同时不影响详情页和 RAG 引用展示。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentEmbeddingTextAppService {

    /** 当前 embedding 文本构造策略版本。 */
    public static final String VERSION = "embedding-text.v2";

    private static final int CONTEXT_PREVIEW_LENGTH = 160;

    private final DocumentRepository documentRepository;
    private final DocumentChunkStorageService chunkStorageService;

    /**
     * 为指定文档的所有 chunk 构造并持久化 embedding_text。
     *
     * @param documentId 文档 ID
     * @return 已更新的 chunk 数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int buildAndStore(Long documentId) {
        DocumentDO document = documentRepository.findById(documentId);
        if (document == null) {
            throw new BaseException(ErrorCodeEnum.DOCUMENT_NOT_EXIST);
        }

        List<DocumentChunkDO> chunks = chunkStorageService.findByDocumentId(documentId);
        if (chunks == null || chunks.isEmpty()) {
            log.warn("文档块为空，无法构造 embedding 文本: documentId={}", documentId);
            throw new BaseException(ErrorCodeEnum.CHUNK_DATA_EMPTY);
        }

        int totalChunks = chunks.size();
        for (int i = 0; i < totalChunks; i++) {
            DocumentChunkDO chunk = chunks.get(i);
            String embeddingText = buildEmbeddingText(document.getTitle(), chunk, totalChunks,
                    previousChunk(chunks, i), nextChunk(chunks, i));
            chunk.setEmbeddingText(embeddingText);
            chunk.setEmbeddingTextVersion(VERSION);
            chunkStorageService.updateById(chunk);
        }

        log.info("embedding 文本构造完成: documentId={}, chunkCount={}, version={}",
                documentId, totalChunks, VERSION);
        return totalChunks;
    }

    /**
     * 构造单个 chunk 的 embedding 输入文本。
     * <p>只做低风险清洗和上下文补充，不改写原文语义。</p>
     */
    public String buildEmbeddingText(String documentTitle, DocumentChunkDO chunk, int totalChunks) {
        return buildEmbeddingText(documentTitle, chunk, totalChunks, null, null);
    }

    /**
     * 构造单个 chunk 的 embedding 输入文本，并补充邻近块短上下文。
     */
    public String buildEmbeddingText(String documentTitle,
                                     DocumentChunkDO chunk,
                                     int totalChunks,
                                     DocumentChunkDO previous,
                                     DocumentChunkDO next) {
        String title = normalizeText(documentTitle);
        String chunkText = normalizeText(chunk.getChunkText());
        int chunkIndex = chunk.getChunkOrder() != null ? chunk.getChunkOrder() + 1 : 1;

        StringBuilder builder = new StringBuilder();
        appendLine(builder, "文档标题", title);
        appendLine(builder, "章节标题", normalizeText(chunk.getSectionTitle()));
        appendLine(builder, "页码", chunk.getPageNumber() != null ? "第 " + chunk.getPageNumber() + " 页" : "");
        appendLine(builder, "片段位置", "第 " + chunkIndex + "/" + totalChunks + " 段");
        appendLine(builder, "上文摘要", preview(previous));
        appendLine(builder, "下文摘要", preview(next));
        builder.append("正文：\n").append(chunkText);
        return builder.toString().trim();
    }

    /**
     * 统一换行并移除不可见控制字符，避免污染向量输入。
     */
    public String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[\\p{Cntrl}&&[^\\n\\t]]", "")
                .trim();
        return normalized.replaceAll("\\n{3,}", "\n\n");
    }

    private DocumentChunkDO previousChunk(List<DocumentChunkDO> chunks, int index) {
        return index > 0 ? chunks.get(index - 1) : null;
    }

    private DocumentChunkDO nextChunk(List<DocumentChunkDO> chunks, int index) {
        return index < chunks.size() - 1 ? chunks.get(index + 1) : null;
    }

    private String preview(DocumentChunkDO chunk) {
        if (chunk == null) {
            return "";
        }
        String text = normalizeText(chunk.getChunkText()).replace('\n', ' ');
        if (text.length() <= CONTEXT_PREVIEW_LENGTH) {
            return text;
        }
        return text.substring(0, CONTEXT_PREVIEW_LENGTH) + "...";
    }

    private void appendLine(StringBuilder builder, String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        builder.append(label).append("：").append(value).append('\n');
    }
}
