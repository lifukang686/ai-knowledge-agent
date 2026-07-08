package com.fukang.knowledge.agent.module.knowledge.model.vo;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 向量化结果
 * <p>封装文档块向量嵌入计算的结果信息，包含每个块的向量数据、
 * 使用的嵌入模型名称、Token 消耗统计等元数据。
 * 作为向量化阶段与存储阶段之间的数据传递对象</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingResult {
    /** 待嵌入的块总数 */
    private int totalChunks;

    /** 向量嵌入列表，与 chunkOrder 一一对应 */
    private List<EmbeddingVector> embeddings;

    /** 使用的嵌入模型名称 */
    private String modelName;

    /** 本次嵌入消耗的总 Token 数 */
    private int totalTokens;

    /** 向量化元数据，如模型版本、维度等 */
    private Map<String, Object> metadata;

    /** 嵌入计算完成时间 */
    private LocalDateTime embeddingTime;

    /** 是否全部嵌入成功 */
    private boolean allSucceeded;

    /** 本次向量化实际使用的模型配置 ID。 */
    public Long modelId() {
        Object value = metadata != null ? metadata.get("modelId") : null;
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Long.valueOf(text);
        }
        return null;
    }

    /** 本次向量化的向量维度，优先读取元数据，缺失时从首个向量推断。 */
    public int dimension() {
        Object value = metadata != null ? metadata.get("vectorDimension") : null;
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Integer.parseInt(text);
        }
        return embeddings.isEmpty() ? 0 : embeddings.get(0).getDimension();
    }

    /** 本次向量化的模型版本标识；当前以模型名称兜底。 */
    public String modelVersion() {
        Object value = metadata != null ? metadata.get("modelVersion") : null;
        return value != null ? String.valueOf(value) : modelName;
    }

    /**
     * 创建全部成功的向量化结果
     *
     * @param embeddings   向量嵌入列表
     * @param modelName    嵌入模型名称
     * @param totalTokens  Token 消耗数
     * @param metadata     元数据
     * @return 全部成功的向量化结果
     */
    public static EmbeddingResult allSuccess(
            List<EmbeddingVector> embeddings,
            String modelName,
            int totalTokens,
            Map<String, Object> metadata) {
        return new EmbeddingResult(
                embeddings.size(),
                List.copyOf(embeddings),
                modelName,
                totalTokens,
                metadata != null ? Map.copyOf(metadata) : Map.of(),
                LocalDateTime.now(),
                true
        );
    }

}
