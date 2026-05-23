package com.fukang.knowledge.agent.infrastructure.ai;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring AI EmbeddingModel 到 langchain4j EmbeddingModel 的适配器
 * <p>将 Spring AI 的向量嵌入能力桥接到 langchain4j 生态中，使 PgVectorEmbeddingStore
 * 等 langchain4j 组件能够复用已有的 Spring AI 嵌入模型实例</p>
 */
@Slf4j
public class SpringAiEmbeddingModelAdapter implements EmbeddingModel {

    private final org.springframework.ai.embedding.EmbeddingModel delegate;

    public SpringAiEmbeddingModelAdapter(org.springframework.ai.embedding.EmbeddingModel delegate) {
        this.delegate = delegate;
    }

    @Override
    public Response<Embedding> embed(String text) {
        log.debug("适配器单文本嵌入: textLength={}", text.length());
        EmbeddingRequest request = new EmbeddingRequest(List.of(text), null);
        org.springframework.ai.embedding.EmbeddingResponse response = delegate.call(request);
        float[] vector = response.getResults().get(0).getOutput();
        return Response.from(Embedding.from(vector));
    }

    @Override
    public Response<List<Embedding>> embedAll(List<TextSegment> textSegments) {
        log.debug("适配器批量嵌入: segmentCount={}", textSegments.size());
        List<String> texts = textSegments.stream()
                .map(TextSegment::text)
                .toList();

        EmbeddingRequest request = new EmbeddingRequest(texts, null);
        org.springframework.ai.embedding.EmbeddingResponse response = delegate.call(request);

        List<Embedding> embeddings = new ArrayList<>(response.getResults().size());
        for (org.springframework.ai.embedding.Embedding aiEmbedding : response.getResults()) {
            embeddings.add(Embedding.from(aiEmbedding.getOutput()));
        }
        return Response.from(embeddings);
    }
}