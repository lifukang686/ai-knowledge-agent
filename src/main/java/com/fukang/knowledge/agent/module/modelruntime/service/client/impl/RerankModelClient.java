package com.fukang.knowledge.agent.module.modelruntime.service.client.impl;

import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.module.modelruntime.service.manager.DynamicModelManager;
import com.fukang.knowledge.agent.module.modelruntime.service.client.RerankClient;
import com.fukang.knowledge.agent.module.rag.model.vo.SearchResult;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.scoring.ScoringModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RerankModelClient implements RerankClient {

    private final DynamicModelManager modelManager;

    /**
     * 调用外部 Rerank 模型返回候选片段分数。
     * 模型未配置或调用失败时返回 Optional.empty，由上层 RerankService 使用本地规则降级。
     */
    @Override
    public Optional<List<RerankScore>> rerank(String query, List<SearchResult> candidates) {
        if (query == null || query.isBlank() || candidates == null || candidates.isEmpty()) {
            return Optional.empty();
        }

        try {
            ScoringModel scoringModel = modelManager.getRerankModel();
            Response<List<Double>> response = scoringModel.scoreAll(toSegments(candidates), query);
            List<Double> scores = response != null ? response.content() : null;
            return toRerankScores(candidates, scores);
        } catch (BaseException e) {
            log.debug("Rerank model unavailable, falling back to local rerank: {}", e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Rerank model call failed, falling back to local rerank: candidates={}", candidates.size(), e);
            return Optional.empty();
        }
    }

    /**
     * ScoringModel 只关心文本内容，RAG 元数据仍保留在 SearchResult 中由上层排序结果携带。
     */
    private List<TextSegment> toSegments(List<SearchResult> candidates) {
        return candidates.stream()
                .map(SearchResult::getChunkText)
                .map(text -> text != null ? text : "")
                .map(TextSegment::from)
                .toList();
    }

    /**
     * 将模型返回的分数按原候选顺序绑定回 chunkId，便于上层重建排序结果。
     */
    private Optional<List<RerankScore>> toRerankScores(List<SearchResult> candidates, List<Double> scores) {
        if (scores == null || scores.isEmpty()) {
            return Optional.empty();
        }

        int count = Math.min(candidates.size(), scores.size());
        List<RerankScore> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Double score = scores.get(i);
            if (score != null) {
                result.add(new RerankScore(i, candidates.get(i).getChunkId(), score));
            }
        }
        return result.isEmpty() ? Optional.empty() : Optional.of(result);
    }
}
