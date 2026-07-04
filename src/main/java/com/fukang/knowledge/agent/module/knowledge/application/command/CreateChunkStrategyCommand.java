package com.fukang.knowledge.agent.module.knowledge.application.command;

/**
 * 创建分块策略命令。
 */
public record CreateChunkStrategyCommand(
        String strategyName,
        String chunkType,
        Integer maxSegmentSize,
        Integer overlapSize
) {}
