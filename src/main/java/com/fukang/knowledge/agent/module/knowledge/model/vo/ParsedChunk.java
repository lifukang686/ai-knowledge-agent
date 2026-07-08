package com.fukang.knowledge.agent.module.knowledge.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 文档分块后的单个文本片段。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParsedChunk {
    private int chunkOrder;

    private String chunkText;

    private int tokenCount;

    private Map<String, String> metadata;

    /**
     * 基于字符数估算 token 数量。
     */
    public static int estimateTokenCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return (int) Math.ceil(text.length() * 0.7);
    }
}
