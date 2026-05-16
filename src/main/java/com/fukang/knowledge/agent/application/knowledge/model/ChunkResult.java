package com.fukang.knowledge.agent.application.knowledge.model;

import java.util.List;
import java.util.Map;

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
public record ChunkResult(
        String documentTitle,
        int totalChunks,
        List<DocumentChunk> chunks,
        String strategyName,
        Map<String, Object> chunkMetadata
) {

    /**
     * 单个文档块
     * <p>表示文档分块后的一个文本片段，包含文本内容和块级别元数据</p>
     *
     * @param chunkOrder  块在文档中的顺序，从 0 开始
     * @param chunkText   块文本内容
     * @param tokenCount  块的 token 估算数量（基于字符数粗略估算）
     * @param metadata    块级别元数据，如所在页码、段落位置等
     */
    public record DocumentChunk(
            int chunkOrder,
            String chunkText,
            int tokenCount,
            Map<String, String> metadata
    ) {

        /**
         * 基于字符数估算 token 数量
         * <p>中英文混合场景下，按字符数的 70% 粗略估算 token 数，
         * 后续集成实际分词器后可替换为精确计算</p>
         *
         * @param text 文本内容
         * @return 估算的 token 数量
         */
        public static int estimateTokenCount(String text) {
            if (text == null || text.isEmpty()) {
                return 0;
            }
            return (int) Math.ceil(text.length() * 0.7);
        }
    }
}