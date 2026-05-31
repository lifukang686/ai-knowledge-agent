package com.fukang.knowledge.agent.infrastructure.rag;

import com.fukang.knowledge.agent.application.ai.port.RerankModelPort;
import com.fukang.knowledge.agent.domain.rag.model.SearchResult;
import com.fukang.knowledge.agent.domain.rag.service.Reranker;
import com.huaban.analysis.jieba.JiebaSegmenter;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.aggregator.DefaultContentAggregator;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RerankService implements Reranker {

    private static final double WEIGHT_VECTOR = 0.35;
    private static final double WEIGHT_RRF = 0.20;
    private static final double WEIGHT_BM25 = 0.10;
    private static final double WEIGHT_COVERAGE = 0.15;
    private static final double WEIGHT_PHRASE = 0.10;
    private static final double WEIGHT_IDF = 0.07;
    private static final double WEIGHT_POSITION = 0.03;

    private static final Set<String> CHINESE_STOP_WORDS = Set.of(
            "的", "了", "在", "是", "我", "有", "和", "就", "不", "人",
            "都", "一", "一个", "上", "也", "很", "到", "说", "要", "去",
            "你", "会", "着", "没有", "看", "好", "自己", "这"
    );

    private static final int MIN_TERM_LENGTH = 2;
    private static final JiebaSegmenter SEGMENTER = new JiebaSegmenter();
    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\\u4e00-\\u9fff]");

    private final RerankModelPort rerankModelPort;

    public RerankService(RerankModelPort rerankModelPort) {
        this.rerankModelPort = rerankModelPort;
    }

    private record ScoredCandidate(
            SearchResult candidate,
            double finalScore,
            double vectorScore,
            double rrfScore,
            double bm25Score,
            double termCoverage,
            double phraseMatch,
            double idfScore,
            double positionScore
    ) {
    }

    public RetrievalAugmentor buildReRankingAugmentor(ContentRetriever contentRetriever, Double minScore) {
        log.info("构建 RAG 增强器: minScore={}, contentAggregator=DefaultContentAggregator", minScore);
        return DefaultRetrievalAugmentor.builder()
                .contentRetriever(contentRetriever)
                .contentAggregator(new DefaultContentAggregator())
                .build();
    }

    @Override
    public List<SearchResult> rerank(List<SearchResult> candidates, String query) {
        if (candidates == null || candidates.isEmpty() || query == null || query.isBlank()) {
            return candidates != null ? candidates : List.of();
        }

        long startTime = System.currentTimeMillis();
        Optional<List<SearchResult>> modelReranked = rerankByModel(candidates, query, startTime);
        if (modelReranked.isPresent()) {
            return modelReranked.get();
        }

        return rerankByLocalRules(candidates, query, startTime);
    }

    private Optional<List<SearchResult>> rerankByModel(List<SearchResult> candidates, String query, long startTime) {
        Optional<List<RerankModelPort.RerankScore>> scoreResult = rerankModelPort.rerank(query, candidates);
        if (scoreResult.isEmpty()) {
            return Optional.empty();
        }

        Map<Integer, Double> scoresByIndex = scoreResult.get().stream()
                .collect(Collectors.toMap(RerankModelPort.RerankScore::index,
                        RerankModelPort.RerankScore::score,
                        (left, right) -> left,
                        LinkedHashMap::new));

        if (scoresByIndex.isEmpty()) {
            return Optional.empty();
        }

        List<SearchResult> reranked = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            SearchResult candidate = candidates.get(i);
            Double modelScore = scoresByIndex.get(i);
            reranked.add(candidate.withScores(candidate.vectorScore(), candidate.bm25Score(),
                    candidate.rrfScore(), modelScore));
        }

        reranked.sort(Comparator.comparingDouble((SearchResult result) ->
                result.rerankScore() != null ? result.rerankScore() : Double.NEGATIVE_INFINITY).reversed());
        log.info("模型重排完成: candidates={}, scored={}, elapsedMs={}",
                candidates.size(), scoresByIndex.size(), System.currentTimeMillis() - startTime);
        return Optional.of(reranked);
    }

    private List<SearchResult> rerankByLocalRules(List<SearchResult> candidates, String query, long startTime) {
        List<String> queryTerms = extractQueryTerms(query);
        if (queryTerms.isEmpty()) {
            return new ArrayList<>(candidates);
        }

        Map<String, Double> idfMap = computeIDF(candidates, queryTerms);
        List<ScoredCandidate> scoredResults = new ArrayList<>();
        for (SearchResult candidate : candidates) {
            String text = candidate.chunkText() != null ? candidate.chunkText().toLowerCase() : "";

            double vectorScore = scoreOrZero(candidate.vectorScore());
            double rrfScore = scoreOrZero(candidate.rrfScore());
            double bm25Score = normalizeBm25(scoreOrZero(candidate.bm25Score()));
            double termCoverage = computeTermCoverage(queryTerms, text);
            double phraseMatch = computePhraseMatch(query, text);
            double idfScore = computeIDFScore(queryTerms, text, idfMap);
            double positionScore = computePositionScore(queryTerms, text);

            double finalScore = WEIGHT_VECTOR * vectorScore
                    + WEIGHT_RRF * rrfScore
                    + WEIGHT_BM25 * bm25Score
                    + WEIGHT_COVERAGE * termCoverage
                    + WEIGHT_PHRASE * phraseMatch
                    + WEIGHT_IDF * idfScore
                    + WEIGHT_POSITION * positionScore;

            scoredResults.add(new ScoredCandidate(candidate, finalScore, vectorScore, rrfScore, bm25Score,
                    termCoverage, phraseMatch, idfScore, positionScore));
        }

        scoredResults.sort(Comparator.comparingDouble(ScoredCandidate::finalScore).reversed());

        List<SearchResult> result = scoredResults.stream()
                .map(r -> r.candidate().withScores(
                        r.candidate().vectorScore(),
                        r.candidate().bm25Score(),
                        r.candidate().rrfScore(),
                        r.finalScore()
                ))
                .collect(Collectors.toList());

        log.debug("重排完成: candidates={}, queryTerms={}, elapsedMs={}",
                candidates.size(), queryTerms.size(), System.currentTimeMillis() - startTime);
        return result;
    }

    private double scoreOrZero(Double score) {
        return score != null ? score : 0.0;
    }

    private double normalizeBm25(double bm25Score) {
        if (bm25Score <= 0.0) {
            return 0.0;
        }
        return bm25Score / (bm25Score + 1.0);
    }

    private List<String> extractQueryTerms(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String normalized = query.toLowerCase();
        String[] tokens = containsChinese(normalized)
                ? SEGMENTER.sentenceProcess(normalized).toArray(new String[0])
                : normalized.split("[\\s,，。?？!！;；:：]+");

        return Arrays.stream(tokens)
                .map(String::trim)
                .filter(token -> !token.isEmpty() && token.length() >= MIN_TERM_LENGTH)
                .filter(token -> !CHINESE_STOP_WORDS.contains(token))
                .distinct()
                .collect(Collectors.toList());
    }

    private Map<String, Double> computeIDF(List<SearchResult> candidates, List<String> queryTerms) {
        int totalDocs = candidates.size();
        Map<String, Double> idfMap = new LinkedHashMap<>();

        for (String term : queryTerms) {
            int docFreq = 0;
            for (SearchResult candidate : candidates) {
                String text = candidate.chunkText() != null ? candidate.chunkText().toLowerCase() : "";
                if (text.contains(term)) {
                    docFreq++;
                }
            }
            idfMap.put(term, Math.log(1.0 + (totalDocs - docFreq + 0.5) / (docFreq + 0.5)));
        }

        return idfMap;
    }

    private double computeTermCoverage(List<String> queryTerms, String text) {
        if (queryTerms.isEmpty()) {
            return 0.0;
        }
        long matched = queryTerms.stream().filter(text::contains).count();
        return (double) matched / queryTerms.size();
    }

    private double computePhraseMatch(String query, String text) {
        if (query == null || query.isBlank() || text == null || text.isBlank()) {
            return 0.0;
        }

        String lowerQuery = query.toLowerCase().trim();
        if (text.contains(lowerQuery)) {
            return 1.0;
        }

        String[] words = segmentQuery(lowerQuery);
        if (words.length == 0) {
            return 0.0;
        }
        if (words.length == 1) {
            return text.contains(words[0]) ? 1.0 : 0.0;
        }

        int maxConsecutive = 0;
        int currentConsecutive = 0;
        for (String word : words) {
            if (text.contains(word)) {
                currentConsecutive++;
            } else {
                maxConsecutive = Math.max(maxConsecutive, currentConsecutive);
                currentConsecutive = 0;
            }
        }
        maxConsecutive = Math.max(maxConsecutive, currentConsecutive);

        return (double) maxConsecutive / words.length;
    }

    private String[] segmentQuery(String query) {
        if (containsChinese(query)) {
            return SEGMENTER.sentenceProcess(query).toArray(new String[0]);
        }
        return query.split("\\s+");
    }

    private boolean containsChinese(String text) {
        return CHINESE_PATTERN.matcher(text).find();
    }

    private double computeIDFScore(List<String> queryTerms, String text, Map<String, Double> idfMap) {
        double matchedIDFSum = 0.0;
        double totalIDFSum = 0.0;

        for (String term : queryTerms) {
            double idf = idfMap.getOrDefault(term, 0.0);
            totalIDFSum += idf;
            if (text.contains(term)) {
                matchedIDFSum += idf;
            }
        }

        if (totalIDFSum == 0.0) {
            return 0.0;
        }
        return matchedIDFSum / totalIDFSum;
    }

    private double computePositionScore(List<String> queryTerms, String text) {
        if (queryTerms.isEmpty() || text == null || text.isBlank()) {
            return 0.0;
        }

        double totalPositionScore = 0.0;
        int matchedCount = 0;

        for (String term : queryTerms) {
            int firstPos = text.indexOf(term);
            if (firstPos >= 0) {
                totalPositionScore += Math.min(1.0, 1.0 / (1.0 + firstPos / 100.0));
                matchedCount++;
            }
        }

        return matchedCount == 0 ? 0.0 : totalPositionScore / matchedCount;
    }
}
