package com.fukang.knowledge.agent.infrastructure.chunk.impl;

import com.fukang.knowledge.agent.common.enums.ChunkTypeEnum;
import com.fukang.knowledge.agent.infrastructure.chunk.AbstractChunkStrategy;
import com.fukang.knowledge.agent.infrastructure.chunk.splitter.MarkdownContentOwnershipSplitter;
import dev.langchain4j.data.document.DocumentSplitter;

/**
 * 按内容归属分块策略。
 */
public class ContentOwnershipChunkStrategy extends AbstractChunkStrategy {

    /**
     * 创建按内容归属分块策略。
     */
    public ContentOwnershipChunkStrategy(String strategyName, int maxSegmentSize, int overlapSize) {
        super(strategyName, ChunkTypeEnum.CONTENT_OWNERSHIP.getCode(), maxSegmentSize, overlapSize);
    }

    /**
     * 使用标题归属分块器切分文本。
     */
    @Override
    protected DocumentSplitter createSplitter(int maxSegmentSize, int overlapSize) {
        return new MarkdownContentOwnershipSplitter(maxSegmentSize, overlapSize);
    }
}
