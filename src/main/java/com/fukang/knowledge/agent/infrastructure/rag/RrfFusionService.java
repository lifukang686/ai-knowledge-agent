package com.fukang.knowledge.agent.infrastructure.rag;

import com.fukang.knowledge.agent.domain.rag.model.SearchResult;
import com.fukang.knowledge.agent.domain.rag.service.ResultFusionStrategy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RRF 检索结果融合服务。
 * <p>将向量检索和全文检索的排序结果按 Reciprocal Rank Fusion 合并，
 * 同时保留原始 vectorScore、bm25Score，便于后续 rerank 和排查分数来源。</p>
 */
@Service
public class RrfFusionService implements ResultFusionStrategy {

    /**
     * 融合向量检索和 BM25 检索结果。
     *
     * @param vectorResults 向量检索结果，按向量相似度排序
     * @param bm25Results   全文检索结果，按 BM25/全文分数排序
     * @param topK          最终返回数量
     * @param rrfK          RRF 平滑参数，值越大排名贡献越平缓
     * @return 按 rrfScore 降序排列的融合结果
     */
    @Override
    public List<SearchResult> fuse(List<SearchResult> vectorResults,
                                   List<SearchResult> bm25Results,
                                   int topK,
                                   int rrfK) {
        Map<Long, FusionCandidate> candidates = new LinkedHashMap<>();

        // 向量结果贡献 RRF 分数，同时保留原始向量相似度。
        for (int i = 0; i < vectorResults.size(); i++) {
            SearchResult result = vectorResults.get(i);
            FusionCandidate candidate = candidates.computeIfAbsent(result.chunkId(),
                    ignored -> new FusionCandidate(result.chunkId(), result.chunkText(), result.metadata()));
            candidate.vectorScore = result.vectorScore() != null ? result.vectorScore() : result.similarity();
            candidate.rrfScore += rrfContribution(i, rrfK);
        }

        // BM25 结果贡献 RRF 分数，同时保留原始全文检索分数。
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

    /**
     * 计算单个排名位置对 RRF 的贡献。
     */
    private double rrfContribution(int zeroBasedRank, int rrfK) {
        return 1.0 / (rrfK + zeroBasedRank + 1);
    }

    /**
     * 融合过程中的临时候选项。
     * <p>同一个 chunk 可能同时来自向量和 BM25，两路分数在此合并。</p>
     */
    private static class FusionCandidate {
        /** 文档块 ID，用于两路结果去重合并。 */
        private final Long chunkId;
        /** 展示给用户和 LLM 的原始 chunk 文本。 */
        private final String chunkText;
        /** 检索来源元数据。 */
        private final String metadata;
        /** 原始向量相似度分数。 */
        private Double vectorScore;
        /** 原始全文检索分数。 */
        private Double bm25Score;
        /** RRF 融合后的排序分数。 */
        private double rrfScore;

        private FusionCandidate(Long chunkId, String chunkText, String metadata) {
            this.chunkId = chunkId;
            this.chunkText = chunkText;
            this.metadata = metadata;
        }

        /**
         * 转换为统一检索结果，当前排序分数使用 rrfScore。
         */
        private SearchResult toSearchResult() {
            return new SearchResult(chunkId, chunkText, rrfScore, metadata)
                    .withScores(vectorScore, bm25Score, rrfScore, null);
        }
    }
}
