package com.fukang.knowledge.agent.infrastructure.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * 文档分块配置属性
 * <p>绑定 application.yml 中 knowledge-agent.chunking 配置段。
 * 支持段落级、句子级、固定长度三种 LangChain4j 分割策略。</p>
 */
@Data
@Validated
@Component
@ConfigurationProperties(prefix = "knowledge-agent.chunking")
public class ChunkingProperties {

    /** 分割策略：paragraph / sentence / fixed */
    @NotBlank
    private String strategy;

    /** 段落分块参数。 */
    @Valid
    @NotNull
    private SplitterProperties paragraph;

    /** 句子分块参数。 */
    @Valid
    @NotNull
    private SplitterProperties sentence;

    /** 固定字符分块参数。 */
    @Valid
    @NotNull
    private SplitterProperties fixed;

    /**
     * 当前策略对应的参数。
     */
    public SplitterProperties active() {
        return switch (strategy) {
            case "sentence" -> sentence;
            case "fixed" -> fixed;
            case "paragraph" -> paragraph;
            default -> paragraph;
        };
    }

    /**
     * LangChain4j DocumentSplitter 通用参数。
     */
    @Data
    public static class SplitterProperties {
        /** 单个 chunk 最大字符数。 */
        @Min(1)
        private int maxSegmentSize;
        /** 相邻 chunk 重叠字符数。 */
        @Min(0)
        private int overlapSize;
    }
}
