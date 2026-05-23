package com.fukang.knowledge.agent.infrastructure.chunk;

import com.fukang.knowledge.agent.application.knowledge.chunk.model.ChunkResult;
import com.fukang.knowledge.agent.application.knowledge.chunk.model.ChunkResult.DocumentChunk;
import com.fukang.knowledge.agent.application.knowledge.parsing.model.DocumentParseResult;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.rag.config.ChunkingProperties;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.document.splitter.DocumentBySentenceSplitter;
import dev.langchain4j.data.segment.TextSegment;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于 langchain4j 的文档分块策略
 * <p>使用 langchain4j 内置的层次化文档分割器实现智能分块。
 * 支持三种分割模式：段落级递归分割、句子级分割、固定大小分割</p>
 */
@Slf4j
public class Langchain4jChunkStrategy implements ChunkStrategy {

    private final ChunkingProperties chunkingProperties;

    public Langchain4jChunkStrategy(ChunkingProperties chunkingProperties) {
        this.chunkingProperties = chunkingProperties;
    }

    @Override
    public String strategyName() {
        return "langchain4j-" + chunkingProperties.getStrategy();
    }

    @Override
    public ChunkResult chunk(DocumentParseResult parseResult) {
        if (parseResult == null || parseResult.isEmpty()) {
            log.warn("文档内容为空，无法分块: title={}",
                    parseResult != null ? parseResult.title() : "null");
            throw new BaseException(ErrorCodeEnum.DOCUMENT_CONTENT_EMPTY);
        }

        String content = parseResult.content();
        Metadata metadata = new Metadata();
        if (parseResult.metadata() != null) {
            parseResult.metadata().forEach(metadata::put);
        }
        Document document = Document.from(content, metadata);

        DocumentSplitter splitter = createSplitter();
        List<TextSegment> segments = splitter.split(document);

        List<DocumentChunk> chunks = new ArrayList<>();
        for (int i = 0; i < segments.size(); i++) {
            TextSegment segment = segments.get(i);
            String segmentText = segment.text();
            Map<String, String> chunkMeta = new HashMap<>();
            segment.metadata().toMap().forEach((k, v) -> chunkMeta.put(k, v.toString()));

            chunks.add(new DocumentChunk(i, segmentText,
                    DocumentChunk.estimateTokenCount(segmentText), chunkMeta));
        }

        log.info("langchain4j 分块完成: title={}, strategy={}, totalChunks={}, maxSegmentSize={}, overlap={}",
                parseResult.title(), strategyName(), chunks.size(),
                chunkingProperties.getMaxSegmentSize(), chunkingProperties.getOverlapSize());

        Map<String, Object> chunkMetadata = new HashMap<>();
        chunkMetadata.put("chunkSize", chunkingProperties.getChunkSize());
        chunkMetadata.put("overlapSize", chunkingProperties.getOverlapSize());
        chunkMetadata.put("strategy", chunkingProperties.getStrategy());
        chunkMetadata.put("maxSegmentSize", chunkingProperties.getMaxSegmentSize());
        chunkMetadata.put("avgTokenCount", calculateAverageTokenCount(chunks));

        return new ChunkResult(
                parseResult.title(), chunks.size(), chunks, strategyName(), chunkMetadata);
    }

    private DocumentSplitter createSplitter() {
        String strategy = chunkingProperties.getStrategy();
        int maxSegmentSize = chunkingProperties.getMaxSegmentSize();
        int overlapSize = chunkingProperties.getOverlapSize();
        int chunkSize = chunkingProperties.getChunkSize();

        return switch (strategy) {
            case "paragraph" -> new DocumentByParagraphSplitter(maxSegmentSize, overlapSize);
            case "sentence" -> new DocumentByParagraphSplitter(maxSegmentSize, overlapSize,
                    new DocumentBySentenceSplitter(maxSegmentSize, overlapSize));
            case "fixed" -> new DocumentByParagraphSplitter(chunkSize, overlapSize);
            default -> {
                log.warn("未知的分块策略: {}，使用默认段落策略", strategy);
                yield new DocumentByParagraphSplitter(maxSegmentSize, overlapSize);
            }
        };
    }

    private double calculateAverageTokenCount(List<DocumentChunk> chunks) {
        if (chunks.isEmpty()) {
            return 0;
        }
        return chunks.stream()
                .mapToInt(DocumentChunk::tokenCount)
                .average()
                .orElse(0);
    }
}