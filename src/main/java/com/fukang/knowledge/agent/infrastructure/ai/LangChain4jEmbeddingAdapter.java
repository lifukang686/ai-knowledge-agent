package com.fukang.knowledge.agent.infrastructure.ai;

import com.fukang.knowledge.agent.application.ai.port.EmbeddingPort;
import com.fukang.knowledge.agent.domain.knowledge.model.EmbeddingResult.EmbeddingVector;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.ModelConfigDO;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * LangChain4j Embedding 模型适配器。
 */
@Component
@RequiredArgsConstructor
public class LangChain4jEmbeddingAdapter implements EmbeddingPort {

    private final DynamicModelManager modelManager;

    @Override
    public BatchResult embedBatch(ModelConfigDO modelConfig, List<String> texts, int chunkOrderOffset) {
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
