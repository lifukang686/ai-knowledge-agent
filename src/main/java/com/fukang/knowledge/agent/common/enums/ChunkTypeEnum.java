package com.fukang.knowledge.agent.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 文档分块类型。
 */
@Getter
@AllArgsConstructor
public enum ChunkTypeEnum {

    /**
     * 按内容归属分块。
     */
    CONTENT_OWNERSHIP("按内容归属"),
    /**
     * 按段落分块。
     */
    PARAGRAPH("按段落"),
    /**
     * 按句子分块。
     */
    SENTENCE("按句子"),
    /**
     * 按字符分块。
     */
    CHARACTER("按字符");

    private final String code;

    /**
     * 按编码解析分块类型。
     */
    public static ChunkTypeEnum fromCode(String code) {
        for (ChunkTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的分块类型: " + code);
    }

}
