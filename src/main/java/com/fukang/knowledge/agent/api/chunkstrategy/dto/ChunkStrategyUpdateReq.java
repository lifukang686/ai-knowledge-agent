package com.fukang.knowledge.agent.api.chunkstrategy.dto;

import jakarta.validation.constraints.Min;

/**
 * 更新分块策略请求。
 */
public record ChunkStrategyUpdateReq(
        String strategyName,
        String chunkType,
        @Min(value = 1, message = "最大字符数必须大于 0")
        Integer maxSegmentSize,
        @Min(value = 0, message = "重叠字符数不能小于 0")
        Integer overlapSize
) {}
