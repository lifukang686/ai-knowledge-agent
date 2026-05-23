package com.fukang.knowledge.agent.infrastructure.ai;

import com.fukang.knowledge.agent.common.enums.ModelTypeEnum;
import com.fukang.knowledge.agent.rag.config.VectorStoreProperties;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

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
    private final DynamicModelManager dynamicModelManager;

    public Langchain4jEmbeddingStoreFactory(DataSource dataSource,
                                            VectorStoreProperties vectorStoreProperties,
                                            DynamicModelManager dynamicModelManager) {
        this.dataSource = dataSource;
        this.vectorStoreProperties = vectorStoreProperties;
        this.dynamicModelManager = dynamicModelManager;
    }

    public PgVectorEmbeddingStore createEmbeddingStore(EmbeddingModel embeddingModel) {
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

    public PgVectorEmbeddingStore createEmbeddingStore() {
        log.info("无参创建 PgVectorEmbeddingStore，自动从 DynamicModelManager 获取嵌入模型");

        org.springframework.ai.embedding.EmbeddingModel springAiEmbeddingModel =
                dynamicModelManager.getEmbeddingModel(ModelTypeEnum.EMBEDDING);

        SpringAiEmbeddingModelAdapter adapter = new SpringAiEmbeddingModelAdapter(springAiEmbeddingModel);

        return createEmbeddingStore(adapter);
    }
}