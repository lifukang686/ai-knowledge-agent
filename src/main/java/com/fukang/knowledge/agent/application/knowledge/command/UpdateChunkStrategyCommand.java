package com.fukang.knowledge.agent.application.knowledge.command;

/**
 * 更新分块策略命令。
 */
public record UpdateChunkStrategyCommand(
        String strategyName,
        String chunkType,
        Integer maxSegmentSize,
        Integer overlapSize
) {}
