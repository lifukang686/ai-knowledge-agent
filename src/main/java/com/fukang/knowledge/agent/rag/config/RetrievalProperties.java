package com.fukang.knowledge.agent.rag.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 语义检索配置属性
 * <p>绑定 application.yml 中 knowledge-agent.retrieval 配置段</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "knowledge-agent.retrieval")
public class RetrievalProperties {
    /** 默认返回结果数量 */
    private int topK = 8;
    /** 默认相似度阈值 (0.0 ~ 1.0)，低于此值的结果将被过滤 */
    private double similarityThreshold = 0.6;
}