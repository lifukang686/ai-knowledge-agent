package com.fukang.knowledge.agent.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 文档分块类型。
 */
@Getter
@AllArgsConstructor
public enum ChunkTypeEnum {

    CONTENT_OWNERSHIP("按内容归属"),
    PARAGRAPH("按段落"),
    SENTENCE("按句子"),
    CHARACTER("按字符");

    private final String code;

    public static ChunkTypeEnum fromCode(String code) {
        for (ChunkTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的分块类型: " + code);
    }

}
