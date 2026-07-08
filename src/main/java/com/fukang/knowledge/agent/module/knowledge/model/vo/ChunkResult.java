package com.fukang.knowledge.agent.module.knowledge.model.vo;


import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档分块结果
 * <p>封装分块处理后的文本片段列表和分块策略的统计信息，
 * 作为分块阶段与向量化阶段之间的数据传递对象</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChunkResult {
    /** 原始文档标题 */
    private String documentTitle;

    /** 分块总数 */
    private int totalChunks;

    /** 分块列表，按文档顺序排列 */
    private List<ParsedChunk> chunks;

    /** 使用的分块策略名称 */
    private String strategyName;

    /** 分块级别的元数据，如平均 token 数、分块参数等 */
    private Map<String, Object> chunkMetadata;

}
