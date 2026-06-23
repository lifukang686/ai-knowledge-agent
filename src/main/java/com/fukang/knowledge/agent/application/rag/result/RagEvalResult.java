package com.fukang.knowledge.agent.application.rag.result;

import com.fukang.knowledge.agent.domain.rag.model.SearchResult;

import java.util.List;

/**
 * RAG 评测执行结果，包含问答结果和检索链路 trace。
 */
public record RagEvalResult(
        String answer,
        String rewrittenQuery,
        String status,
        List<SearchResult> retrievedChunks,
        List<SearchResult> rerankedChunks,
        long latencyMs
) {
}
