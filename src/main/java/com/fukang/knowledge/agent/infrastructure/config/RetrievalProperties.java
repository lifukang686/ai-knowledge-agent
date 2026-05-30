package com.fukang.knowledge.agent.infrastructure.config;

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
    /** 是否启用混合检索（向量 + BM25） */
    private boolean hybridEnabled = true;
    /** BM25 全文检索的最低分数阈值 */
    private double bm25Threshold = 0.1;
    /** RRF 融合参数 k */
    private int rrfK = 60;
}