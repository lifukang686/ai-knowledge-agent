package com.fukang.knowledge.agent.infrastructure.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fukang.knowledge.agent.application.ai.port.RerankModelPort;
import com.fukang.knowledge.agent.common.enums.ModelTypeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.domain.rag.model.SearchResult;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.ModelConfigDO;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.ModelProviderDO;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.scoring.ScoringModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * LangChain4j ScoringModel 重排适配器。
 * <p>当前通过 HTTP ScoringModel 接入模型，后续可替换为官方 Jina/Cohere/Voyage 等实现。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LangChain4jScoringRerankModelAdapter implements RerankModelPort {

    private final ModelResolutionService modelResolutionService;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<List<RerankScore>> rerank(String query, List<SearchResult> candidates) {
        if (query == null || query.isBlank() || candidates == null || candidates.isEmpty()) {
            return Optional.empty();
        }

        try {
            ModelProviderDO preferredProvider = modelResolutionService.resolveProvider();
            ModelConfigDO rerankModel = modelResolutionService.resolveModelConfig(
                    preferredProvider.getId(), ModelTypeEnum.RERANK);
            ModelProviderDO provider = modelResolutionService.getModelProviderById(rerankModel.getProviderId());
            ScoringModel scoringModel = new HttpScoringModel(provider, rerankModel, objectMapper);

            List<TextSegment> segments = candidates.stream()
                    .map(SearchResult::chunkText)
                    .map(text -> text != null ? text : "")
                    .map(TextSegment::from)
                    .toList();
            Response<List<Double>> response = scoringModel.scoreAll(segments, query);
            List<RerankScore> scores = toRerankScores(response.content(), candidates);
            return scores.isEmpty() ? Optional.empty() : Optional.of(scores);
        } catch (BaseException e) {
            log.debug("未找到可用 RERANK 模型配置，跳过模型重排: {}", e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Rerank ScoringModel 调用异常，降级到本地规则重排", e);
            return Optional.empty();
        }
    }

    private List<RerankScore> toRerankScores(List<Double> scores, List<SearchResult> candidates) {
        if (scores == null || scores.isEmpty()) {
            return List.of();
        }
        int count = Math.min(scores.size(), candidates.size());
        List<RerankScore> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Double score = scores.get(i);
            if (score != null) {
                result.add(new RerankScore(i, candidates.get(i).chunkId(), score));
            }
        }
        return result;
    }
}
