package com.fukang.knowledge.agent.infrastructure.chunk;

import com.fukang.knowledge.agent.common.enums.ChunkTypeEnum;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;

/**
 * 按段落分块策略。
 */
public class ParagraphChunkStrategy extends AbstractChunkStrategy {

    /**
     * 创建按段落分块策略。
     */
    public ParagraphChunkStrategy(String strategyName, int maxSegmentSize, int overlapSize) {
        super(strategyName, ChunkTypeEnum.PARAGRAPH.getCode(), maxSegmentSize, overlapSize);
    }

    /**
     * 使用段落分块器切分文本。
     */
    @Override
    protected DocumentSplitter createSplitter(int maxSegmentSize, int overlapSize) {
        return new DocumentByParagraphSplitter(maxSegmentSize, overlapSize);
    }
}
