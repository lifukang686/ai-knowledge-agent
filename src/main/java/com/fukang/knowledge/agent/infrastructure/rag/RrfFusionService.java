package com.fukang.knowledge.agent.infrastructure.rag;

import com.fukang.knowledge.agent.domain.rag.model.SearchResult;
import com.fukang.knowledge.agent.domain.rag.service.ResultFusionStrategy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RrfFusionService implements ResultFusionStrategy {

    @Override
    public List<SearchResult> fuse(List<SearchResult> vectorResults,
                                   List<SearchResult> bm25Results,
                                   int topK,
                                   int rrfK) {
        Map<Long, FusionCandidate> candidates = new LinkedHashMap<>();

        for (int i = 0; i < vectorResults.size(); i++) {
            SearchResult result = vectorResults.get(i);
            FusionCandidate candidate = candidates.computeIfAbsent(result.chunkId(),
                    ignored -> new FusionCandidate(result.chunkId(), result.chunkText(), result.metadata()));
            candidate.vectorScore = result.vectorScore() != null ? result.vectorScore() : result.similarity();
            candidate.rrfScore += rrfContribution(i, rrfK);
        }

        for (int i = 0; i < bm25Results.size(); i++) {
            SearchResult result = bm25Results.get(i);
            FusionCandidate candidate = candidates.computeIfAbsent(result.chunkId(),
                    ignored -> new FusionCandidate(result.chunkId(), result.chunkText(), result.metadata()));
            candidate.bm25Score = result.bm25Score() != null ? result.bm25Score() : result.similarity();
            candidate.rrfScore += rrfContribution(i, rrfK);
        }

        return candidates.values().stream()
                .sorted(Comparator.comparingDouble((FusionCandidate c) -> c.rrfScore).reversed())
                .limit(topK)
                .map(FusionCandidate::toSearchResult)
                .toList();
    }

    private double rrfContribution(int zeroBasedRank, int rrfK) {
        return 1.0 / (rrfK + zeroBasedRank + 1);
    }

    private static class FusionCandidate {
        private final Long chunkId;
        private final String chunkText;
        private final String metadata;
        private Double vectorScore;
        private Double bm25Score;
        private double rrfScore;

        private FusionCandidate(Long chunkId, String chunkText, String metadata) {
            this.chunkId = chunkId;
            this.chunkText = chunkText;
            this.metadata = metadata;
        }

        private SearchResult toSearchResult() {
            return new SearchResult(chunkId, chunkText, rrfScore, metadata)
                    .withScores(vectorScore, bm25Score, rrfScore, null);
        }
    }
}
