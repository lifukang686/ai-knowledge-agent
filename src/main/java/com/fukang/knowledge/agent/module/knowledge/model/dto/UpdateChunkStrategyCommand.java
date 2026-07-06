package com.fukang.knowledge.agent.module.knowledge.model.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新分块策略命令。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateChunkStrategyCommand {
    private String strategyName;

    private String chunkType;

    private Integer maxSegmentSize;

    private Integer overlapSize;

}
