package com.fukang.knowledge.agent.infrastructure.ai;

import com.fukang.knowledge.agent.infrastructure.config.VectorStoreProperties;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * langchain4j 向量存储工厂
 * <p>负责创建和管理 PgVectorEmbeddingStore 实例，将 langchain4j 的向量存储能力
 * 集成到项目的动态模型体系中。支持通过 EmbeddingModel 参数或无参方式创建存储实例</p>
 */
@Slf4j
@Component
public class Langchain4jEmbeddingStoreFactory {

    private final DataSource dataSource;
    private final VectorStoreProperties vectorStoreProperties;

    public Langchain4jEmbeddingStoreFactory(DataSource dataSource,
                                            VectorStoreProperties vectorStoreProperties) {
        this.dataSource = dataSource;
        this.vectorStoreProperties = vectorStoreProperties;
    }

    /**
     * 启动时初始化向量存储表
     * <p>通过创建 PgVectorEmbeddingStore 实例触发建表（CREATE TABLE IF NOT EXISTS），
     * 确保应用启动后表已就绪，无需等待首次文档上传</p>
     */
    @PostConstruct
    void initTable() {
        log.info("应用启动，初始化 pgvector 向量表: table={}, dimension={}, dropTableFirst={}",
                vectorStoreProperties.getTableName(), vectorStoreProperties.getDimension(),
                vectorStoreProperties.isDropTableFirst());
        PgVectorEmbeddingStore.datasourceBuilder()
                .datasource(dataSource)
                .table(vectorStoreProperties.getTableName())
                .dimension(vectorStoreProperties.getDimension())
                .createTable(true)
                .dropTableFirst(vectorStoreProperties.isDropTableFirst())
                .build();
        log.info("pgvector 向量表初始化完成: table={}", vectorStoreProperties.getTableName());
    }

    public PgVectorEmbeddingStore createEmbeddingStore() {
        log.info("创建 PgVectorEmbeddingStore: table={}, dimension={}, indexType={}",
                vectorStoreProperties.getTableName(),
                vectorStoreProperties.getDimension(),
                vectorStoreProperties.getIndexType());

        return PgVectorEmbeddingStore.datasourceBuilder()
                .datasource(dataSource)
                .table(vectorStoreProperties.getTableName())
                .dimension(vectorStoreProperties.getDimension())
                .createTable(vectorStoreProperties.isCreateTable())
                .dropTableFirst(vectorStoreProperties.isDropTableFirst())
                .build();
    }

    public void deleteByKnowledgeBaseId(Long knowledgeBaseId) {
        String tableName = vectorStoreProperties.getTableName();
        String sql = "DELETE FROM " + tableName + " WHERE metadata::json->>'knowledge_base_id' = ?";
        log.info("通过 pgvector 删除向量: table={}, knowledgeBaseId={}", tableName, knowledgeBaseId);

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, String.valueOf(knowledgeBaseId));
            int deleted = ps.executeUpdate();
            log.info("pgvector 向量删除完成: knowledgeBaseId={}, deletedCount={}", knowledgeBaseId, deleted);
        } catch (SQLException e) {
            log.error("pgvector 删除向量失败: knowledgeBaseId={}", knowledgeBaseId, e);
            throw new RuntimeException("pgvector 删除向量失败", e);
        }
    }

    public void deleteByChunkIds(List<Long> chunkIds) {
        if (chunkIds == null || chunkIds.isEmpty()) {
            return;
        }

        String tableName = vectorStoreProperties.getTableName();
        String placeholders = String.join(",", chunkIds.stream().map(id -> "?").toArray(String[]::new));
        String sql = "DELETE FROM " + tableName + " WHERE metadata::json->>'chunk_id' IN (" + placeholders + ")";
        log.info("通过 pgvector 批量删除向量: table={}, chunkCount={}", tableName, chunkIds.size());

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < chunkIds.size(); i++) {
                ps.setString(i + 1, String.valueOf(chunkIds.get(i)));
            }
            int deleted = ps.executeUpdate();
            log.info("pgvector 批量向量删除完成: chunkCount={}, deletedCount={}", chunkIds.size(), deleted);
        } catch (SQLException e) {
            log.error("pgvector 批量删除向量失败: chunkCount={}", chunkIds.size(), e);
            throw new RuntimeException("pgvector 批量删除向量失败", e);
        }
    }
}
