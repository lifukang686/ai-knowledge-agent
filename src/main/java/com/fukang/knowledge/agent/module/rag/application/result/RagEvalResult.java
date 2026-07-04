package com.fukang.knowledge.agent.module.rag.application.result;

import com.fukang.knowledge.agent.module.rag.domain.model.SearchResult;

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
