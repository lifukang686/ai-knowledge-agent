package com.fukang.knowledge.agent.domain.rag.model;

/**
 * 语义检索结果 DTO
 *
 * @param chunkId   文档块 ID
 * @param chunkText 文档块文本内容
 * @param similarity 余弦相似度分数 (0.0 ~ 1.0)
 * @param metadata  向量元数据（模型版本、向量维度等 JSON 格式）
 */
public record SearchResult(
        Long chunkId,
        String chunkText,
        double similarity,
        String metadata
) {}