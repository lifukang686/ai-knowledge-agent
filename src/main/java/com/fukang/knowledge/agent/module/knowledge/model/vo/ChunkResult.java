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
 *
 * @param documentTitle     原始文档标题
 * @param totalChunks       分块总数
 * @param chunks            分块列表，按文档顺序排列
 * @param strategyName      使用的分块策略名称
 * @param chunkMetadata     分块级别的元数据，如平均 token 数、分块参数等
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChunkResult {
    private String documentTitle;

    private int totalChunks;

    private List<ParsedChunk> chunks;

    private String strategyName;

    private Map<String, Object> chunkMetadata;

}
