package com.fukang.knowledge.agent.domain.knowledge.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 文档处理状态枚举
 * <p>定义文档从上传到处理完成的完整生命周期状态，支持终态和中间态判断</p>
 */
@Getter
@AllArgsConstructor
public enum DocumentStatus {

    PENDING("pending", "待处理"),
    PARSING("parsing", "解析中"),
    CHUNKING("chunking", "分块中"),
    EMBEDDING("embedding", "向量化中"),
    COMPLETED("completed", "处理完成"),
    FAILED("failed", "处理失败");

    private final String code;
    private final String description;

    /** 是否为终态（不再变化） */
    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED;
    }

    /** 是否为中间态（处理中，可能因重启中断） */
    public boolean isProcessing() {
        return this == PARSING || this == CHUNKING || this == EMBEDDING;
    }
}