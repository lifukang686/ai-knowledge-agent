package com.fukang.knowledge.agent.api.chunkstrategy.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 分块策略响应。
 */
public record ChunkStrategyResp(
        Long id,
        String strategyName,
        String chunkType,
        Integer maxSegmentSize,
        Integer overlapSize,
        Boolean isDefault,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        LocalDateTime createTime,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        LocalDateTime updateTime
) {}
