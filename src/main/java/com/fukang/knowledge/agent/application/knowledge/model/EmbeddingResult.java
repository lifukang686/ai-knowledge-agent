package com.fukang.knowledge.agent.application.knowledge.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 向量化结果
 * <p>封装文档块向量嵌入计算的结果信息，包含每个块的向量数据、
 * 使用的嵌入模型名称、Token 消耗统计等元数据。
 * 作为向量化阶段与存储阶段之间的数据传递对象</p>
 *
 * @param totalChunks    待嵌入的块总数
 * @param embeddings     向量嵌入列表，与 chunkOrder 一一对应
 * @param modelName      使用的嵌入模型名称
 * @param totalTokens    本次嵌入消耗的总 Token 数
 * @param metadata       向量化元数据，如模型版本、维度等
 * @param embeddingTime  嵌入计算完成时间
 * @param allSucceeded   是否全部嵌入成功
 */
public record EmbeddingResult(
        int totalChunks,
        List<EmbeddingVector> embeddings,
        String modelName,
        int totalTokens,
        java.util.Map<String, Object> metadata,
        LocalDateTime embeddingTime,
        boolean allSucceeded
) {

    /**
     * 单个向量嵌入数据
     * <p>映射 chunkOrder → vector 的对应关系</p>
     *
     * @param chunkOrder 块在文档中的顺序号，从 0 开始
     * @param vector     向量数据，float 数组表示
     * @param dimension  向量维度
     */
    public record EmbeddingVector(
            int chunkOrder,
            float[] vector,
            int dimension
    ) {
        public EmbeddingVector {
            java.util.Objects.requireNonNull(vector, "向量数据不能为空");
            if (dimension <= 0) {
                dimension = vector.length;
            }
        }
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
            java.util.Map<String, Object> metadata) {
        return new EmbeddingResult(
                embeddings.size(),
                List.copyOf(embeddings),
                modelName,
                totalTokens,
                metadata != null ? java.util.Map.copyOf(metadata) : java.util.Map.of(),
                LocalDateTime.now(),
                true
        );
    }

    /**
     * 创建部分失败的向量化结果
     *
     * @param embeddings   已成功嵌入的向量列表
     * @param totalChunks  待嵌入的块总数
     * @param modelName    嵌入模型名称
     * @param totalTokens  Token 消耗数
     * @param metadata     元数据
     * @return 部分失败的向量化结果
     */
    public static EmbeddingResult partialSuccess(
            List<EmbeddingVector> embeddings,
            int totalChunks,
            String modelName,
            int totalTokens,
            java.util.Map<String, Object> metadata) {
        return new EmbeddingResult(
                totalChunks,
                embeddings != null ? List.copyOf(embeddings) : List.of(),
                modelName,
                totalTokens,
                metadata != null ? java.util.Map.copyOf(metadata) : java.util.Map.of(),
                LocalDateTime.now(),
                embeddings != null && embeddings.size() == totalChunks
        );
    }
}