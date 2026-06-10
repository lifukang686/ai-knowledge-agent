package com.fukang.knowledge.agent.application.knowledge.command;

/**
 * 创建分块策略命令。
 */
public record CreateChunkStrategyCommand(
        String strategyName,
        String chunkType,
        Integer maxSegmentSize,
        Integer overlapSize
) {}
