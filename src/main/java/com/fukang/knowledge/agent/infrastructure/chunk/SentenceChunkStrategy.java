package com.fukang.knowledge.agent.infrastructure.chunk;

import com.fukang.knowledge.agent.common.enums.ChunkTypeEnum;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentBySentenceSplitter;

/**
 * 按句子分块策略。
 */
public class SentenceChunkStrategy extends AbstractChunkStrategy {

    /**
     * 创建按句子分块策略。
     */
    public SentenceChunkStrategy(String strategyName, int maxSegmentSize, int overlapSize) {
        super(strategyName, ChunkTypeEnum.SENTENCE.getCode(), maxSegmentSize, overlapSize);
    }

    /**
     * 使用句子分块器切分文本。
     */
    @Override
    protected DocumentSplitter createSplitter(int maxSegmentSize, int overlapSize) {
        return new DocumentBySentenceSplitter(maxSegmentSize, overlapSize);
    }
}
