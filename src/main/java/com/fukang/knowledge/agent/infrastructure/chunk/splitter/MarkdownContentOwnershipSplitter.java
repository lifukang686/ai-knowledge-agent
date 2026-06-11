package com.fukang.knowledge.agent.infrastructure.chunk.splitter;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 保持 Markdown 标题与所属正文同块的分块器。
 */
public class MarkdownContentOwnershipSplitter implements DocumentSplitter {

    private static final Pattern MARKDOWN_HEADING = Pattern.compile("^(#{1,6})\\s+(.+?)\\s*$");
    private static final String SECTION_TITLE_KEY = "sectionTitle";

    private final int maxSegmentSize;
    private final int overlapSize;

    public MarkdownContentOwnershipSplitter(int maxSegmentSize, int overlapSize) {
        this.maxSegmentSize = maxSegmentSize;
        this.overlapSize = overlapSize;
    }

    @Override
    public List<TextSegment> split(Document document) {
        if (document == null || document.text() == null || document.text().isBlank()) {
            return List.of();
        }

        List<Section> sections = parseSections(document.text());
        List<TextSegment> segments = new ArrayList<>();
        for (Section section : sections) {
            splitSection(section, document.metadata(), segments);
        }
        return segments;
    }

    /**
     * 按 Markdown 标题划分内容归属区间。
     */
    private List<Section> parseSections(String text) {
        List<Section> sections = new ArrayList<>();
        String currentTitle = null;
        StringBuilder current = new StringBuilder();

        for (String line : text.split("\\R", -1)) {
            Matcher headingMatcher = MARKDOWN_HEADING.matcher(line.trim());
            if (headingMatcher.matches() && !current.isEmpty()) {
                sections.add(new Section(currentTitle, current.toString().trim()));
                current.setLength(0);
            }
            if (headingMatcher.matches()) {
                currentTitle = headingMatcher.group(2).trim();
            }
            current.append(line).append('\n');
        }

        if (!current.isEmpty()) {
            sections.add(new Section(currentTitle, current.toString().trim()));
        }
        return sections;
    }

    /**
     * 章节过长时按段落续切，并保留标题上下文。
     */
    private void splitSection(Section section, Metadata documentMetadata, List<TextSegment> segments) {
        if (section.text().length() <= maxSegmentSize) {
            addSegment(section.text(), section.title(), documentMetadata, segments);
            return;
        }

        String heading = firstHeadingLine(section.text());
        String body = heading == null ? section.text() : section.text().substring(heading.length()).trim();
        int bodyLimit = bodyLimit(heading);
        List<String> paragraphs = splitParagraphs(body);
        StringBuilder current = new StringBuilder();

        for (String paragraph : paragraphs) {
            String candidate = appendParagraph(current, paragraph);
            if (candidate.length() > bodyLimit && !current.isEmpty()) {
                addSegment(withHeading(heading, current.toString()), section.title(), documentMetadata, segments);
                current = new StringBuilder(overlapSuffix(current.toString()));
            }
            if (paragraph.length() > bodyLimit) {
                flushCurrent(heading, section.title(), documentMetadata, segments, current);
                splitLongText(heading, paragraph, section.title(), documentMetadata, segments);
            } else {
                if (!current.isEmpty()) {
                    current.append("\n\n");
                }
                current.append(paragraph);
            }
        }

        flushCurrent(heading, section.title(), documentMetadata, segments, current);
    }

    private void flushCurrent(String heading, String title, Metadata documentMetadata,
                              List<TextSegment> segments, StringBuilder current) {
        if (!current.isEmpty()) {
            addSegment(withHeading(heading, current.toString()), title, documentMetadata, segments);
            current.setLength(0);
        }
    }

    private void splitLongText(String heading, String text, String title, Metadata documentMetadata,
                               List<TextSegment> segments) {
        int bodyLimit = bodyLimit(heading);
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(text.length(), start + bodyLimit);
            addSegment(withHeading(heading, text.substring(start, end)), title, documentMetadata, segments);
            if (end == text.length()) {
                break;
            }
            start = Math.max(end - overlapSize, start + 1);
        }
    }

    /**
     * 预留标题占用的块长度。
     */
    private int bodyLimit(String heading) {
        if (heading == null || heading.isBlank()) {
            return maxSegmentSize;
        }
        return Math.max(1, maxSegmentSize - heading.length() - 2);
    }

    /**
     * 创建文本段并继承文档元数据。
     */
    private void addSegment(String text, String title, Metadata documentMetadata, List<TextSegment> segments) {
        String cleanText = text == null ? "" : text.trim();
        if (cleanText.isBlank()) {
            return;
        }
        Metadata metadata = documentMetadata == null ? new Metadata() : documentMetadata.copy();
        if (title != null && !title.isBlank()) {
            metadata.put(SECTION_TITLE_KEY, title);
        }
        segments.add(TextSegment.from(cleanText, metadata));
    }

    /**
     * 拼接段落预览。
     */
    private String appendParagraph(StringBuilder current, String paragraph) {
        if (current.isEmpty()) {
            return paragraph;
        }
        return current + "\n\n" + paragraph;
    }

    /**
     * 按空行拆分段落。
     */
    private List<String> splitParagraphs(String text) {
        List<String> paragraphs = new ArrayList<>();
        for (String paragraph : text.split("(?:\\R\\s*){2,}")) {
            if (!paragraph.isBlank()) {
                paragraphs.add(paragraph.trim());
            }
        }
        return paragraphs;
    }

    /**
     * 提取章节标题行。
     */
    private String firstHeadingLine(String text) {
        int lineEnd = text.indexOf('\n');
        String firstLine = lineEnd >= 0 ? text.substring(0, lineEnd).trim() : text.trim();
        return MARKDOWN_HEADING.matcher(firstLine).matches() ? firstLine : null;
    }

    /**
     * 为子块补回标题。
     */
    private String withHeading(String heading, String text) {
        if (heading == null || heading.isBlank()) {
            return text;
        }
        String cleanText = text == null ? "" : text.trim();
        if (cleanText.startsWith(heading)) {
            return cleanText;
        }
        return heading + "\n\n" + cleanText;
    }

    /**
     * 生成相邻子块重叠文本。
     */
    private String overlapSuffix(String text) {
        if (overlapSize <= 0 || text.length() <= overlapSize) {
            return "";
        }
        return text.substring(text.length() - overlapSize).trim();
    }

    private record Section(String title, String text) {
    }
}
