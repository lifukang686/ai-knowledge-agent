package com.fukang.knowledge.agent.infrastructure.chunk;

import com.fukang.knowledge.agent.common.enums.ChunkTypeEnum;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentByCharacterSplitter;

/**
 * 按字符分块策略。
 */
public class CharacterChunkStrategy extends AbstractChunkStrategy {

    public CharacterChunkStrategy(String strategyName, int maxSegmentSize, int overlapSize) {
        super(strategyName, ChunkTypeEnum.CHARACTER.getCode(), maxSegmentSize, overlapSize);
    }

    @Override
    protected DocumentSplitter createSplitter(int maxSegmentSize, int overlapSize) {
        return new DocumentByCharacterSplitter(maxSegmentSize, overlapSize);
    }
}
