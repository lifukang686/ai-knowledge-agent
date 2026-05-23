package com.fukang.knowledge.agent.rag.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文档分块配置属性
 * <p>绑定 application.yml 中 knowledge-agent.chunking 配置段。
 * 支持段落级、句子级、固定长度三种分割策略</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "knowledge-agent.chunking")
public class ChunkingProperties {
    /** 块大小（字符数/令牌数），默认 800 */
    private int chunkSize = 800;
    /** 块间重叠大小，默认 100 */
    private int overlapSize = 100;
    /** 分割策略：paragraph / sentence / fixed */
    private String strategy = "paragraph";
    /** 段落分割时最大段大小，默认 1000 */
    private int maxSegmentSize = 1000;
}