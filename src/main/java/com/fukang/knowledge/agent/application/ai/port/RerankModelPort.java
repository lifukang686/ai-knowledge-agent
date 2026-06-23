package com.fukang.knowledge.agent.application.ai.port;

import com.fukang.knowledge.agent.domain.rag.model.SearchResult;

import java.util.List;
import java.util.Optional;

/**
 * Rerank 模型调用端口。
 * <p>返回模型对候选文档的相关性评分，调用失败或未配置模型时由上层降级处理。</p>
 */
public interface RerankModelPort {

    /**
     * 对候选片段重排序。
     */
    Optional<List<RerankScore>> rerank(String query, List<SearchResult> candidates);

    /**
     * 单条重排序得分。
     */
    record RerankScore(int index, Long chunkId, double score) {
    }
}
