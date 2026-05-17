package com.fukang.knowledge.agent.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 模型类型枚举
 * <p>定义 AI 模型的具体用途分类，用于区分对话模型、嵌入模型等不同类型</p>
 */
@Getter
@AllArgsConstructor
public enum ModelTypeEnum {

    CHAT("CHAT", "对话模型"),

    EMBEDDING("EMBEDDING", "嵌入模型"),

    RERANK("RERANK", "重排序模型"),

    STT("STT", "语音转文字模型");

    private final String code;

    private final String description;

    public static ModelTypeEnum fromCode(String code) {
        for (ModelTypeEnum type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的模型类型: " + code);
    }
}