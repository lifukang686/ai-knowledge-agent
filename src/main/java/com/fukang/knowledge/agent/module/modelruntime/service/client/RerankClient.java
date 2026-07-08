package com.fukang.knowledge.agent.module.modelruntime.service.client;

import com.fukang.knowledge.agent.module.modelruntime.model.vo.RerankScore;
import com.fukang.knowledge.agent.module.rag.model.vo.SearchResult;

import java.util.List;
import java.util.Optional;

/**
 * Rerank 模型调用端口。
 * <p>返回模型对候选文档的相关性评分，调用失败或未配置模型时由上层降级处理。</p>
 */
public interface RerankClient {

    /**
     * 对候选片段重排序。
     */
    Optional<List<RerankScore>> rerank(String query, List<SearchResult> candidates);
}
