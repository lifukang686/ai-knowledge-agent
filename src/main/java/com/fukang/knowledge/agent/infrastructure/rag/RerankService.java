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

/**
 * RAG 重排服务。
 * <p>优先调用外部重排模型；模型不可用时使用向量分、全文分、词覆盖率和位置特征做本地重排。</p>
 */
@Slf4j
@Component
public class RerankService implements Reranker {

    /** 向量相似度权重，体现语义召回相关性。 */
    private static final double WEIGHT_VECTOR = 0.35;
    /** RRF 融合分权重，体现多路召回综合排名。 */
    private static final double WEIGHT_RRF = 0.20;
    /** BM25 分权重，体现关键词匹配相关性。 */
    private static final double WEIGHT_BM25 = 0.10;
    /** 关键词覆盖率权重，体现问题词命中比例。 */
    private static final double WEIGHT_COVERAGE = 0.15;
    /** 短语匹配权重，体现连续词片段命中程度。 */
    private static final double WEIGHT_PHRASE = 0.10;
    /** IDF 分权重，体现重要问题词命中程度。 */
    private static final double WEIGHT_IDF = 0.07;
    /** 位置分权重，命中越靠前得分越高。 */
    private static final double WEIGHT_POSITION = 0.03;

    /** 中文停用词，避免常见虚词影响本地重排。 */
    private static final Set<String> CHINESE_STOP_WORDS = Set.of(
            "的", "了", "在", "是", "我", "有", "和", "就", "不", "人",
            "都", "一", "一个", "上", "也", "很", "到", "说", "要", "去",
            "你", "会", "着", "没有", "看", "好", "自己", "这"
    );

    /** 最短关键词长度，过滤过短噪声词。 */
    private static final int MIN_TERM_LENGTH = 2;
    /** 中文分词器，用于抽取查询关键词。 */
    private static final JiebaSegmenter SEGMENTER = new JiebaSegmenter();
    /** 中文字符检测，用于选择分词方式。 */
    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\\u4e00-\\u9fff]");

    private final RerankModelPort rerankModelPort;

    /**
     * 创建重排服务。
     *
     * @param rerankModelPort 外部重排模型端口
     */
    public RerankService(RerankModelPort rerankModelPort) {
        this.rerankModelPort = rerankModelPort;
    }

    /**
     * 本地规则重排的候选分数明细。
     *
     * @param candidate     原始检索候选
     * @param finalScore    本地融合后的最终分
     * @param vectorScore   向量相似度分
     * @param rrfScore      RRF 融合分
     * @param bm25Score     归一化后的 BM25 分
     * @param termCoverage  查询关键词覆盖率
     * @param phraseMatch   查询短语匹配度
     * @param idfScore      重要关键词命中分
     * @param positionScore 关键词位置分
     */
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

    /**
     * 构建 LangChain4j 检索增强器，保留默认聚合能力。
     *
     * @param contentRetriever LangChain4j 内容检索器
     * @param minScore         最低分阈值，仅用于日志观察
     */
    public RetrievalAugmentor buildReRankingAugmentor(ContentRetriever contentRetriever, Double minScore) {
        log.info("构建 RAG 增强器: minScore={}, contentAggregator=DefaultContentAggregator", minScore);
        return DefaultRetrievalAugmentor.builder()
                .contentRetriever(contentRetriever)
                .contentAggregator(new DefaultContentAggregator())
                .build();
    }

    /**
     * 对召回候选进行重排。
     *
     * @param candidates 原始召回候选
     * @param query      用户查询或改写查询
     */
    @Override
    public List<SearchResult> rerank(List<SearchResult> candidates, String query) {
        if (candidates == null || candidates.isEmpty() || query == null || query.isBlank()) {
            return candidates != null ? candidates : List.of();
        }

        long startTime = System.currentTimeMillis();
        // 外部模型分数更贴近语义相关性，成功时直接作为最终重排依据。
        Optional<List<SearchResult>> modelReranked = rerankByModel(candidates, query, startTime);
        if (modelReranked.isPresent()) {
            return modelReranked.get();
        }

        return rerankByLocalRules(candidates, query, startTime);
    }

    /**
     * 使用外部重排模型排序。
     *
     * @param candidates 原始召回候选
     * @param query      用户查询或改写查询
     * @param startTime  重排开始时间，用于统计耗时
     */
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

    /**
     * 使用本地规则兜底排序。
     *
     * @param candidates 原始召回候选
     * @param query      用户查询或改写查询
     * @param startTime  重排开始时间，用于统计耗时
     */
    private List<SearchResult> rerankByLocalRules(List<SearchResult> candidates, String query, long startTime) {
        List<String> queryTerms = extractQueryTerms(query);
        if (queryTerms.isEmpty()) {
            return new ArrayList<>(candidates);
        }

        Map<String, Double> idfMap = computeIDF(candidates, queryTerms);
        List<ScoredCandidate> scoredResults = new ArrayList<>();
        for (SearchResult candidate : candidates) {
            // 候选文档块文本，统一转小写便于关键词匹配。
            String text = candidate.chunkText() != null ? candidate.chunkText().toLowerCase() : "";

            // 向量召回分，衡量语义相似度。
            double vectorScore = scoreOrZero(candidate.vectorScore());
            // RRF 融合分，衡量多路召回后的综合排名。
            double rrfScore = scoreOrZero(candidate.rrfScore());
            // BM25 关键词分，归一化后参与融合。
            double bm25Score = normalizeBm25(scoreOrZero(candidate.bm25Score()));
            // 查询词覆盖率，命中的问题关键词越多分越高。
            double termCoverage = computeTermCoverage(queryTerms, text);
            // 短语匹配度，连续命中查询片段越多分越高。
            double phraseMatch = computePhraseMatch(query, text);
            // IDF 命中分，命中越稀有的问题词分越高。
            double idfScore = computeIDFScore(queryTerms, text, idfMap);
            // 位置分，关键词首次出现越靠前分越高。
            double positionScore = computePositionScore(queryTerms, text);

            // 本地规则将召回分、关键词覆盖和文本位置融合，作为模型不可用时的兜底排序。
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

    /**
     * 空分数按 0 处理。
     *
     * @param score 原始分数
     */
    private double scoreOrZero(Double score) {
        return score != null ? score : 0.0;
    }

    /**
     * 将 BM25 分压缩到 0~1 区间。
     *
     * @param bm25Score 原始 BM25 分
     */
    private double normalizeBm25(double bm25Score) {
        if (bm25Score <= 0.0) {
            return 0.0;
        }
        return bm25Score / (bm25Score + 1.0);
    }

    /**
     * 抽取查询关键词。
     *
     * @param query 用户查询或改写查询
     */
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

    /**
     * 计算查询词在候选集合中的 IDF。
     *
     * @param candidates 原始召回候选
     * @param queryTerms 查询关键词
     */
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

    /**
     * 计算查询词覆盖率。
     *
     * @param queryTerms 查询关键词
     * @param text       候选文本
     */
    private double computeTermCoverage(List<String> queryTerms, String text) {
        if (queryTerms.isEmpty()) {
            return 0.0;
        }
        long matched = queryTerms.stream().filter(text::contains).count();
        return (double) matched / queryTerms.size();
    }

    /**
     * 计算查询短语匹配度。
     *
     * @param query 用户查询或改写查询
     * @param text  候选文本
     */
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

    /**
     * 按语言切分查询词。
     *
     * @param query 已归一化查询
     */
    private String[] segmentQuery(String query) {
        if (containsChinese(query)) {
            return SEGMENTER.sentenceProcess(query).toArray(new String[0]);
        }
        return query.split("\\s+");
    }

    /**
     * 判断文本是否包含中文。
     *
     * @param text 待检测文本
     */
    private boolean containsChinese(String text) {
        return CHINESE_PATTERN.matcher(text).find();
    }

    /**
     * 计算重要关键词命中分。
     *
     * @param queryTerms 查询关键词
     * @param text       候选文本
     * @param idfMap     查询词 IDF 映射
     */
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

    /**
     * 计算关键词位置分。
     *
     * @param queryTerms 查询关键词
     * @param text       候选文本
     */
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
