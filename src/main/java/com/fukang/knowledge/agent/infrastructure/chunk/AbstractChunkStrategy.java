package com.fukang.knowledge.agent.infrastructure.chunk;

import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.domain.knowledge.model.ChunkResult;
import com.fukang.knowledge.agent.domain.knowledge.model.ChunkResult.DocumentChunk;
import com.fukang.knowledge.agent.domain.knowledge.model.DocumentParseResult;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文档分块策略公共模板。
 */
@Slf4j
public abstract class AbstractChunkStrategy implements ChunkStrategy {

    private static final Pattern PAGE_MARKER = Pattern.compile("\\[\\[PAGE:(\\d+)]]");
    private static final Pattern SECTION_MARKER = Pattern.compile("\\[\\[SECTION:([^]]+)]]");
    private static final Pattern TABLE_MARKER = Pattern.compile("\\[\\[/?TABLE]]");

    private final String strategyName;
    private final String chunkType;
    private final int maxSegmentSize;
    private final int overlapSize;

    protected AbstractChunkStrategy(String strategyName, String chunkType,
                                    int maxSegmentSize, int overlapSize) {
        this.strategyName = strategyName;
        this.chunkType = chunkType;
        this.maxSegmentSize = maxSegmentSize;
        this.overlapSize = overlapSize;
    }

    @Override
    public ChunkResult chunk(DocumentParseResult parseResult) {
        if (parseResult == null || parseResult.isEmpty()) {
            log.warn("文档内容为空，无法分块: title={}", parseResult != null ? parseResult.title() : "null");
            throw new BaseException(ErrorCodeEnum.DOCUMENT_CONTENT_EMPTY);
        }

        Metadata metadata = new Metadata();
        if (parseResult.metadata() != null) {
            parseResult.metadata().forEach(metadata::put);
        }

        Document document = Document.from(parseResult.content(), metadata);
        DocumentSplitter splitter = createSplitter(maxSegmentSize, overlapSize);
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

        log.info("langchain4j 分块完成: title={}, strategy={}, chunkType={}, totalChunks={}, maxSegmentSize={}, overlap={}",
                parseResult.title(), strategyName(), chunkType, chunks.size(), maxSegmentSize, overlapSize);

        Map<String, Object> chunkMetadata = new HashMap<>();
        chunkMetadata.put("strategy", strategyName());
        chunkMetadata.put("chunkType", chunkType);
        chunkMetadata.put("maxSegmentSize", maxSegmentSize);
        chunkMetadata.put("overlapSize", overlapSize);
        chunkMetadata.put("avgTokenCount", calculateAverageTokenCount(chunks));

        return new ChunkResult(parseResult.title(), chunks.size(), chunks, strategyName(), chunkMetadata);
    }

    @Override
    public String strategyName() {
        return strategyName;
    }

    protected abstract DocumentSplitter createSplitter(int maxSegmentSize, int overlapSize);

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

    private Optional<String> inferSectionTitle(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        for (String line : text.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isBlank()
                    || PAGE_MARKER.matcher(trimmed).matches()
                    || TABLE_MARKER.matcher(trimmed).matches()) {
                continue;
            }
            if (isLikelyHeading(trimmed)) {
                return Optional.of(trimmed);
            }
        }
        return Optional.empty();
    }

    private boolean isLikelyHeading(String line) {
        if (line.length() > 80 || line.endsWith("。") || line.endsWith(".") || line.endsWith("；")) {
            return false;
        }
        return line.matches("^第[一二三四五六七八九十百千万0-9]+[章节篇部].*")
                || line.matches("^\\d+(\\.\\d+)*[、\\s].*")
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
