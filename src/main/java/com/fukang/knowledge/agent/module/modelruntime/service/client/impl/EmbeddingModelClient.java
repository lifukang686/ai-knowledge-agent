package com.fukang.knowledge.agent.module.modelruntime.service.client.impl;

import com.fukang.knowledge.agent.module.modelruntime.service.client.EmbeddingClient;
import com.fukang.knowledge.agent.module.modelruntime.service.manager.DynamicModelManager;
import com.fukang.knowledge.agent.module.knowledge.model.vo.EmbeddingResult.EmbeddingVector;
import com.fukang.knowledge.agent.module.model.model.entity.ModelConfigEntity;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * LangChain4j Embedding 妯″瀷閫傞厤鍣ㄣ€? */
@Component
@RequiredArgsConstructor
public class EmbeddingModelClient implements EmbeddingClient {

    private final DynamicModelManager modelManager;

    @Override
    public BatchResult embedBatch(ModelConfigEntity modelConfig, List<String> texts, int chunkOrderOffset) {
        EmbeddingModel embeddingClient = modelManager.getEmbeddingModel(modelConfig);
        List<TextSegment> segments = texts.stream().map(TextSegment::from).toList();
        Response<List<Embedding>> response = embeddingClient.embedAll(segments);

        List<EmbeddingVector> vectors = extractVectors(response.content(), chunkOrderOffset);
        int totalTokens = response.tokenUsage() != null ? response.tokenUsage().totalTokenCount() : 0;
        return new BatchResult(vectors, totalTokens);
    }

    private List<EmbeddingVector> extractVectors(List<Embedding> embeddings, int chunkOrderOffset) {
        List<EmbeddingVector> vectors = new ArrayList<>(embeddings.size());
        for (int i = 0; i < embeddings.size(); i++) {
            float[] embeddingArray = embeddings.get(i).vector();
            vectors.add(new EmbeddingVector(chunkOrderOffset + i, embeddingArray, embeddingArray.length));
        }
        return vectors;
    }
}
