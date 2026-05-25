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
import java.util.regex.Pattern;

/**
 * 句子级分块策略
 * <p>以句子为基本单位，将语义连贯的句子组合成不超过指定大小的文本块。
 * 超长句子内部按字符边界切分以保证每块不超过最大长度限制。
 * 较固定长度策略更适合对语义连贯性要求较高的场景</p>
 */
@Slf4j
public class SentenceChunkStrategy implements ChunkStrategy {

    /** 默认最大块长度（字符数） */
    private static final int DEFAULT_MAX_CHUNK_SIZE = 800;

    /** 中英文句子分割正则，以句号、问号、感叹号、换行等为边界 */
    private static final Pattern SENTENCE_PATTERN = Pattern.compile(
            "(?<=[。.！!？?；;\\n])(?=[^。.！!？?；;\\n])"
    );

    private final int maxChunkSize;

    /**
     * 使用默认最大块长度构造句子分块策略
     * <p>默认 800 字符，适用于大多数嵌入模型</p>
     */
    public SentenceChunkStrategy() {
        this(DEFAULT_MAX_CHUNK_SIZE);
    }

    /**
     * 使用自定义最大块长度构造句子分块策略
     *
     * @param maxChunkSize 最大块长度（字符数）
     */
    public SentenceChunkStrategy(int maxChunkSize) {
        this.maxChunkSize = maxChunkSize;
    }

    @Override
    public String strategyName() {
        return "sentence";
    }

    /**
     * 按句子边界对文档内容执行分块
     * <p>分块流程：
     * <ol>
     *   <li>将文本按句子边界分割为句子列表</li>
     *   <li>依次累加句子直到达到最大块长度，超长句子按字符切分</li>
     *   <li>优先在句子边界处切分块，保证不切断完整句子</li>
     * </ol>
     *
     * @param parseResult 文档解析结果
     * @return 分块结果
     * @throws BaseException 文本为空时抛出异常
     */
    @Override
    public ChunkResult chunk(DocumentParseResult parseResult) {
        if (parseResult.isEmpty()) {
            log.warn("文档内容为空，无法分块: title={}", parseResult.title());
            throw new BaseException(ErrorCodeEnum.DOCUMENT_CONTENT_EMPTY);
        }

        List<String> sentences = splitIntoSentences(parseResult.content());
        List<DocumentChunk> chunks = buildChunks(sentences);

        log.info("句子级分块完成: title={}, strategy={}, totalChunks={}, maxSize={}",
                parseResult.title(), strategyName(), chunks.size(), maxChunkSize);

        Map<String, Object> chunkMetadata = new HashMap<>();
        chunkMetadata.put("maxChunkSize", maxChunkSize);
        chunkMetadata.put("totalSentences", sentences.size());
        chunkMetadata.put("avgTokenCount", calculateAverageTokenCount(chunks));

        return new ChunkResult(
                parseResult.title(), chunks.size(), chunks, strategyName(), chunkMetadata
        );
    }

    /**
     * 将文本按句子边界分割
     * <p>使用正则匹配句子边界（句号、问号、感叹号、换行后），
     * 过滤掉纯空白句子并 trim 每个句子</p>
     *
     * @param content 原始文本
     * @return 句子列表
     */
    private List<String> splitIntoSentences(String content) {
        String[] parts = SENTENCE_PATTERN.split(content);
        List<String> sentences = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                sentences.add(trimmed);
            }
        }
        if (sentences.isEmpty()) {
            sentences.add(content);
        }
        return sentences;
    }

    /**
     * 根据句子构建文本块
     * <p>依次累加句子，当累加长度超过最大块长度时切分新块。
     * 超长句子内部按字符边界切分以保证每块不超过限制</p>
     *
     * @param sentences 句子列表
     * @return 文档块列表
     */
    private List<DocumentChunk> buildChunks(List<String> sentences) {
        List<DocumentChunk> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();
        int chunkOrder = 0;

        for (String sentence : sentences) {
            if (sentence.length() > maxChunkSize && !currentChunk.isEmpty()) {
                String chunkText = currentChunk.toString().trim();
                if (!chunkText.isEmpty()) {
                    chunks.add(createChunk(chunkOrder++, chunkText));
                }
                currentChunk = new StringBuilder();
            }

            currentChunk.append(sentence);

            while (currentChunk.length() > maxChunkSize) {
                int splitPoint = maxChunkSize;

                int lastBoundary = Math.max(
                        Math.max(
                                currentChunk.lastIndexOf(".", splitPoint),
                                currentChunk.lastIndexOf("。", splitPoint)
                        ),
                        Math.max(
                                currentChunk.lastIndexOf("!", splitPoint),
                                currentChunk.lastIndexOf("？", splitPoint)
                        )
                );

                if (lastBoundary > maxChunkSize / 2) {
                    splitPoint = lastBoundary + 1;
                }

                String chunkText = currentChunk.substring(0, splitPoint).trim();
                if (!chunkText.isEmpty()) {
                    chunks.add(createChunk(chunkOrder++, chunkText));
                }
                currentChunk = new StringBuilder(currentChunk.substring(splitPoint).trim());
            }

            if (!currentChunk.isEmpty() && chunkOrder > 0) {
                String chunkText = currentChunk.toString().trim();
                currentChunk = new StringBuilder();
                if (!chunkText.isEmpty()) {
                    currentChunk.append(chunkText);
                }
            }
        }

        if (!currentChunk.isEmpty()) {
            String chunkText = currentChunk.toString().trim();
            if (!chunkText.isEmpty()) {
                chunks.add(createChunk(chunkOrder, chunkText));
            }
        }

        return renumberChunks(chunks);
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
    private DocumentChunk createChunk(int order, String text) {
        return new DocumentChunk(order, text, DocumentChunk.estimateTokenCount(text), Map.of());
    }

    /**
     * 计算块的平均 token 数
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