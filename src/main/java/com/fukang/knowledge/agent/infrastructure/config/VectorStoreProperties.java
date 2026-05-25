package com.fukang.knowledge.agent.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 向量存储配置属性
 * <p>绑定 application.yml 中 knowledge-agent.vector-store 配置段，用于 pgvector + langchain4j 集成</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "knowledge-agent.vector-store")
public class VectorStoreProperties {
    private String tableName = "vector_embedding";
    private int dimension = 1024;
    private String indexType = "hnsw";
    private boolean createTable = false;
    private boolean dropTableFirst = false;
}