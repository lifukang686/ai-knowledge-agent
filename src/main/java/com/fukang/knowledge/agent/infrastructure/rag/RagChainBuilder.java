package com.fukang.knowledge.agent.infrastructure.rag;

import com.fukang.knowledge.agent.common.enums.ModelTypeEnum;
import com.fukang.knowledge.agent.infrastructure.ai.Langchain4jEmbeddingStoreFactory;
import com.fukang.knowledge.agent.infrastructure.ai.DynamicModelManager;
import com.fukang.knowledge.agent.infrastructure.config.RetrievalProperties;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * RAG 链构建器
 * <p>负责构建 langchain4j 的 RAG 检索增强组件链：
 * ContentRetriever → RetrievalAugmentor → ChatLanguageModel，
 * 将原本手动编排的 RAG 流程转换为 langchain4j 的声明式链式编排</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RagChainBuilder {

    private final Langchain4jEmbeddingStoreFactory storeFactory;
    private final DynamicModelManager dynamicModelManager;
    private final RetrievalProperties retrievalProperties;

    public ContentRetriever buildContentRetriever(Long knowledgeBaseId) {
        EmbeddingModel embeddingModel = createEmbeddingModel();
        PgVectorEmbeddingStore embeddingStore = storeFactory.createEmbeddingStore(embeddingModel);

        Filter filter = MetadataFilterBuilder.metadataKey("knowledge_base_id").isEqualTo(String.valueOf(knowledgeBaseId));

        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(retrievalProperties.getTopK())
                .minScore(retrievalProperties.getSimilarityThreshold())
                .filter(filter)
                .build();
    }

    public RetrievalAugmentor buildRetrievalAugmentor(ContentRetriever contentRetriever) {
        return DefaultRetrievalAugmentor.builder()
                .contentRetriever(contentRetriever)
                .build();
    }

    public ChatLanguageModel createChatModel() {
        return dynamicModelManager.getChatModel(ModelTypeEnum.CHAT);
    }

    private EmbeddingModel createEmbeddingModel() {
        return dynamicModelManager.getEmbeddingModel(ModelTypeEnum.EMBEDDING);
    }
}