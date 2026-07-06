package com.fukang.knowledge.agent.module.knowledge.model.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.Min;

/**
 * 更新分块策略请求。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChunkStrategyUpdateReq {
    private String strategyName;

    private String chunkType;

    @Min(value = 1, message = "最大字符数必须大于 0")
    private Integer maxSegmentSize;

    @Min(value = 0, message = "重叠字符数不能小于 0")
    private Integer overlapSize;

}
