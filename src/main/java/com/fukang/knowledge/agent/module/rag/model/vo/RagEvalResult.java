package com.fukang.knowledge.agent.module.rag.model.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fukang.knowledge.agent.module.rag.model.vo.SearchResult;

import java.util.List;

/**
 * RAG 评测执行结果，包含问答结果和检索链路 trace。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RagEvalResult {
    private String answer;

    private String rewrittenQuery;

    private String status;

    private List<SearchResult> retrievedChunks;

    private List<SearchResult> rerankedChunks;

    private long latencyMs;

}
