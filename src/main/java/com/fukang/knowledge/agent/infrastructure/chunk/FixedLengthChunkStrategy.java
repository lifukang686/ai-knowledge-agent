package com.fukang.knowledge.agent.infrastructure.chunk;

import com.fukang.knowledge.agent.domain.knowledge.model.ChunkResult;
import com.fukang.knowledge.agent.domain.knowledge.model.ChunkResult.DocumentChunk;
import com.fukang.knowledge.agent.domain.knowledge.model.DocumentParseResult;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 固定长度分块策略
 * <p>按照指定的最大字符数将文档文本切分为大小均匀的文本块。
 * 优先在段落边界处切分，对于超长段落则在字符边界切分。
 * 相邻块之间保留重叠区域以维持上下文连续性。
 * 是 MVP 阶段默认的分块策略，适用于大多数场景</p>
 */
@Slf4j
public class FixedLengthChunkStrategy implements ChunkStrategy {

    /** 默认最大块长度（字符数） */
    private static final int DEFAULT_MAX_CHUNK_SIZE = 1000;

    /** 最小块长度（字符数），小于此长度的块将与相邻块合并 */
    private static final int MIN_CHUNK_SIZE = 100;

    /** 块间重叠字符数，通过重复部分内容来保持上下文连续性 */
    private static final int OVERLAP_SIZE = 100;

    private final int maxChunkSize;
    private final int minChunkSize;
    private final int overlapSize;

    /**
     * 使用默认参数构造分块策略
     * <p>默认最大块长 1000 字符，最小块长 100 字符，重叠 100 字符</p>
     */
    public FixedLengthChunkStrategy() {
        this(DEFAULT_MAX_CHUNK_SIZE, MIN_CHUNK_SIZE, OVERLAP_SIZE);
    }

    /**
     * 使用自定义参数构造分块策略
     *
     * @param maxChunkSize 最大块长度（字符数）
     * @param minChunkSize 最小块长度（字符数），小于该值的块将被合并
     * @param overlapSize  块间重叠字符数
     */
    public FixedLengthChunkStrategy(int maxChunkSize, int minChunkSize, int overlapSize) {
        this.maxChunkSize = maxChunkSize;
        this.minChunkSize = minChunkSize;
        this.overlapSize = overlapSize;
    }

    @Override
    public String strategyName() {
        return "fixed-length";
    }

    /**
     * 对文档内容执行固定长度分块
     * <p>分块流程：
     * <ol>
     *   <li>将文本按段落分割</li>
     *   <li>累加段落直至达到或超过最大块长度</li>
     *   <li>超长段落内部按字符边界切分</li>
     *   <li>相邻块之间保留重叠区域以维持上下文</li>
     * </ol>
     *
     * @param parseResult 文档解析结果
     * @return 分块结果
     * @throws BaseException 文本为空或分块失败时抛出异常
     */
    @Override
    public ChunkResult chunk(DocumentParseResult parseResult) {
        if (parseResult.isEmpty()) {
            log.warn("文档内容为空，无法分块: title={}", parseResult.title());
            throw new BaseException(ErrorCodeEnum.DOCUMENT_CONTENT_EMPTY);
        }

        String content = parseResult.content();
        List<String> paragraphs = splitIntoParagraphs(content);
        List<DocumentChunk> chunks = buildChunks(paragraphs);

        log.info("固定长度分块完成: title={}, strategy={}, totalChunks={}, maxSize={}, overlap={}",
                parseResult.title(), strategyName(), chunks.size(), maxChunkSize, overlapSize);

        Map<String, Object> chunkMetadata = new HashMap<>();
        chunkMetadata.put("maxChunkSize", maxChunkSize);
        chunkMetadata.put("overlapSize", overlapSize);
        chunkMetadata.put("avgTokenCount", calculateAverageTokenCount(chunks));

        return new ChunkResult(
                parseResult.title(), chunks.size(), chunks, strategyName(), chunkMetadata
        );
    }

    /**
     * 将文本按段落拆分
     * <p>以连续换行符为段落分隔符，过滤掉空段落</p>
     *
     * @param content 原始文本
     * @return 段落列表
     */
    private List<String> splitIntoParagraphs(String content) {
        List<String> paragraphs = new ArrayList<>();
        StringBuilder currentParagraph = new StringBuilder();

        for (String line : content.split("\n", -1)) {
            if (line.isBlank()) {
                if (!currentParagraph.isEmpty()) {
                    paragraphs.add(currentParagraph.toString().trim());
                    currentParagraph.setLength(0);
                }
            } else {
                if (!currentParagraph.isEmpty()) {
                    currentParagraph.append('\n');
                }
                currentParagraph.append(line);
            }
        }

        if (!currentParagraph.isEmpty()) {
            paragraphs.add(currentParagraph.toString().trim());
        }

        if (paragraphs.isEmpty()) {
            paragraphs.add(content);
        }

        return paragraphs;
    }

    /**
     * 根据段落构建文本块
     * <p>优先在段落边界处切分，超长段落内部按字符边界切分，
     * 相邻块之间保留重叠内容以维持上下文连续性</p>
     *
     * @param paragraphs 段落列表
     * @return 文档块列表
     */
    private List<DocumentChunk> buildChunks(List<String> paragraphs) {
        List<DocumentChunk> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();
        int chunkOrder = 0;

        for (int paraIndex = 0; paraIndex < paragraphs.size(); paraIndex++) {
            String paragraph = paragraphs.get(paraIndex);
            int estimatedLen = currentChunk.length() + paragraph.length() + 1;

            if (estimatedLen >= maxChunkSize && !currentChunk.isEmpty()) {
                String chunkText = currentChunk.toString().trim();
                if (!chunkText.isEmpty()) {
                    chunks.add(createChunk(chunkOrder++, chunkText,
                            Map.of("paragraphIndex", String.valueOf(paraIndex))));
                }

                int overlapStart = Math.max(0, currentChunk.length() - overlapSize);
                String overlapText = currentChunk.substring(overlapStart);
                currentChunk = new StringBuilder(overlapText);
            }

            if (!currentChunk.isEmpty()) {
                currentChunk.append('\n');
            }
            currentChunk.append(paragraph);

            while (currentChunk.length() > maxChunkSize) {
                int splitPoint = maxChunkSize;

                int newlineIndex = currentChunk.lastIndexOf("\n", splitPoint);
                if (newlineIndex > 0) {
                    splitPoint = newlineIndex;
                }

                String chunkText = currentChunk.substring(0, splitPoint).trim();
                if (!chunkText.isEmpty()) {
                    chunks.add(createChunk(chunkOrder++, chunkText,
                            Map.of("paragraphIndex", String.valueOf(paraIndex))));
                }

                int overlapStartForSplit = Math.max(0, splitPoint - overlapSize);
                currentChunk = new StringBuilder(currentChunk.substring(overlapStartForSplit));
            }
        }

        if (!currentChunk.isEmpty()) {
            String chunkText = currentChunk.toString().trim();
            if (!chunkText.isEmpty()) {
                chunks.add(createChunk(chunkOrder, chunkText,
                        Map.of("endOfDocument", "true")));
            }
        }

        List<DocumentChunk> mergedChunks = mergeSmallChunks(chunks);
        log.debug("分块统计: 原始块数={}, 合并后块数={}", chunks.size(), mergedChunks.size());
        return renumberChunks(mergedChunks);
    }

    /**
     * 将过小的块与相邻块合并
     * <p>遍历所有块，如果某块长度小于最小块长度，则与后一块合并</p>
     *
     * @param chunks 原始块列表
     * @return 合并后的块列表
     */
    private List<DocumentChunk> mergeSmallChunks(List<DocumentChunk> chunks) {
        if (chunks.size() <= 1) {
            return chunks;
        }

        List<DocumentChunk> merged = new ArrayList<>();
        DocumentChunk pending = null;

        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk current = chunks.get(i);

            if (current.chunkText().length() < minChunkSize && i < chunks.size() - 1) {
                if (pending == null) {
                    pending = current;
                }
                DocumentChunk next = chunks.get(i + 1);
                pending = mergeTwoChunks(pending, next);
                i++;
                merged.add(pending);
                pending = null;
            } else if (pending != null) {
                DocumentChunk combined = mergeTwoChunks(pending, current);
                merged.add(combined);
                pending = null;
            } else {
                merged.add(current);
            }
        }

        if (pending != null) {
            merged.add(pending);
        }

        return merged;
    }

    /**
     * 合并两个相邻块
     *
     * @param first  前一个块
     * @param second 后一个块
     * @return 合并后的块
     */
    private DocumentChunk mergeTwoChunks(DocumentChunk first, DocumentChunk second) {
        String combinedText = first.chunkText() + "\n" + second.chunkText();
        Map<String, String> combinedMetadata = new HashMap<>(first.metadata());
        combinedMetadata.putAll(second.metadata());
        combinedMetadata.put("merged", "true");
        return new DocumentChunk(0, combinedText,
                DocumentChunk.estimateTokenCount(combinedText), combinedMetadata);
    }

    /**
     * 对所有块重新编号，保证 chunkOrder 与列表位置一致
     *
     * @param chunks 块列表
     * @return 重新编号后的块列表
     */
    private List<DocumentChunk> renumberChunks(List<DocumentChunk> chunks) {
        List<DocumentChunk> renumbered = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk original = chunks.get(i);
            renumbered.add(new DocumentChunk(i, original.chunkText(),
                    original.tokenCount(), original.metadata()));
        }
        return renumbered;
    }

    /**
     * 创建单个文档块
     */
    private DocumentChunk createChunk(int order, String text, Map<String, String> metadata) {
        return new DocumentChunk(order, text, DocumentChunk.estimateTokenCount(text), metadata);
    }

    /**
     * 计算块的平均 token 数
     *
     * @param chunks 块列表
     * @return 平均 token 数
     */
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