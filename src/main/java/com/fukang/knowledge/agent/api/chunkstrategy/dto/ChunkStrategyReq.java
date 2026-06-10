package com.fukang.knowledge.agent.api.chunkstrategy.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 创建分块策略请求。
 */
public record ChunkStrategyReq(
        @NotBlank(message = "分块策略名称不能为空")
        String strategyName,
        @NotBlank(message = "分块类型不能为空")
        String chunkType,
        @NotNull(message = "最大字符数不能为空")
        @Min(value = 1, message = "最大字符数必须大于 0")
        Integer maxSegmentSize,
        @NotNull(message = "重叠字符数不能为空")
        @Min(value = 0, message = "重叠字符数不能小于 0")
        Integer overlapSize
) {}
