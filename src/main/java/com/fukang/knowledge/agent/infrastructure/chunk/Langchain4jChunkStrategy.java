package com.fukang.knowledge.agent.infrastructure.chunk;

import com.fukang.knowledge.agent.domain.knowledge.model.ChunkResult;
import com.fukang.knowledge.agent.domain.knowledge.model.ChunkResult.DocumentChunk;
import com.fukang.knowledge.agent.domain.knowledge.model.DocumentParseResult;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.infrastructure.config.ChunkingProperties;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.document.splitter.DocumentBySentenceSplitter;
import dev.langchain4j.data.document.splitter.DocumentByCharacterSplitter;
import dev.langchain4j.data.segment.TextSegment;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基于 langchain4j 的文档分块策略
 * <p>使用 langchain4j 内置的层次化文档分割器实现智能分块。
 * 支持三种分割模式：段落级递归分割、句子级分割、固定大小分割</p>
 */
@Slf4j
public class Langchain4jChunkStrategy implements ChunkStrategy {

    private static final Pattern PAGE_MARKER = Pattern.compile("\\[\\[PAGE:(\\d+)]]");
    private static final Pattern SECTION_MARKER = Pattern.compile("\\[\\[SECTION:([^]]+)]]");
    private static final Pattern TABLE_MARKER = Pattern.compile("\\[\\[/?TABLE]]");

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

        // 核心分块点：根据配置创建 splitter，并在这里把完整文档切成多个 TextSegment。
        DocumentSplitter splitter = createSplitter();
        List<TextSegment> segments = splitter.split(document);

        List<DocumentChunk> chunks = new ArrayList<>();
        for (int i = 0; i < segments.size(); i++) {
            TextSegment segment = segments.get(i);
            String segmentText = segment.text();
            Map<String, String> chunkMeta = new HashMap<>();
            segment.metadata().toMap().forEach((k, v) -> chunkMeta.put(k, v.toString()));
            enrichStructureMetadata(segmentText, chunkMeta);
            segmentText = cleanInternalMarkers(segmentText);

            chunks.add(new DocumentChunk(i, segmentText,
                    DocumentChunk.estimateTokenCount(segmentText), chunkMeta));
        }

        log.info("langchain4j 分块完成: title={}, strategy={}, totalChunks={}, maxSegmentSize={}, overlap={}",
                parseResult.title(), strategyName(), chunks.size(),
                chunkingProperties.active().getMaxSegmentSize(), chunkingProperties.active().getOverlapSize());

        Map<String, Object> chunkMetadata = new HashMap<>();
        chunkMetadata.put("strategy", chunkingProperties.getStrategy());
        chunkMetadata.put("maxSegmentSize", chunkingProperties.active().getMaxSegmentSize());
        chunkMetadata.put("overlapSize", chunkingProperties.active().getOverlapSize());
        chunkMetadata.put("avgTokenCount", calculateAverageTokenCount(chunks));

        return new ChunkResult(
                parseResult.title(), chunks.size(), chunks, strategyName(), chunkMetadata);
    }

    private void enrichStructureMetadata(String text, Map<String, String> metadata) {
        Matcher pageMatcher = PAGE_MARKER.matcher(text);
        String lastPage = null;
        while (pageMatcher.find()) {
            lastPage = pageMatcher.group(1);
        }
        if (lastPage != null) {
            metadata.put("pageNumber", lastPage);
        }

        Matcher sectionMatcher = SECTION_MARKER.matcher(text);
        String lastSection = null;
        while (sectionMatcher.find()) {
            lastSection = sectionMatcher.group(1).trim();
        }
        if (lastSection != null && !lastSection.isBlank()) {
            metadata.put("sectionTitle", lastSection);
        } else {
            inferSectionTitle(text).ifPresent(title -> metadata.put("sectionTitle", title));
        }
    }

    private java.util.Optional<String> inferSectionTitle(String text) {
        if (text == null || text.isBlank()) {
            return java.util.Optional.empty();
        }
        for (String line : text.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isBlank()
                    || PAGE_MARKER.matcher(trimmed).matches()
                    || TABLE_MARKER.matcher(trimmed).matches()) {
                continue;
            }
            if (isLikelyHeading(trimmed)) {
                return java.util.Optional.of(trimmed);
            }
        }
        return java.util.Optional.empty();
    }

    private boolean isLikelyHeading(String line) {
        if (line.length() > 80 || line.endsWith("。") || line.endsWith(".") || line.endsWith("；")) {
            return false;
        }
        return line.matches("^(第[一二三四五六七八九十百千万0-9]+[章节篇部分].*)")
                || line.matches("^\\d+(\\.\\d+)*[、.\\s].*")
                || line.startsWith("#");
    }

    private String cleanInternalMarkers(String text) {
        if (text == null) {
            return "";
        }
        return TABLE_MARKER.matcher(
                        SECTION_MARKER.matcher(
                                PAGE_MARKER.matcher(text).replaceAll(""))
                                .replaceAll(""))
                .replaceAll("")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private DocumentSplitter createSplitter() {
        String strategy = chunkingProperties.getStrategy();
        ChunkingProperties.SplitterProperties active = chunkingProperties.active();
        int maxSegmentSize = active.getMaxSegmentSize();
        int overlapSize = active.getOverlapSize();

        // 分块策略来自配置：优先按段落/句子/固定长度切，并用 maxSegmentSize 与 overlapSize 控制块大小和重叠。
        return switch (strategy) {
            // 按段落优先切
            case "paragraph" -> new DocumentByParagraphSplitter(maxSegmentSize, overlapSize);
            // 按句子优先切
            case "sentence" -> new DocumentBySentenceSplitter(maxSegmentSize, overlapSize);
            // 按字符长度切
            case "fixed" -> new DocumentByCharacterSplitter(maxSegmentSize, overlapSize);
            default -> {
                log.warn("未知的分块策略: {}，使用默认段落策略", strategy);
                yield new DocumentByParagraphSplitter(
                        chunkingProperties.getParagraph().getMaxSegmentSize(),
                        chunkingProperties.getParagraph().getOverlapSize());
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
