package com.fukang.knowledge.agent.infrastructure.rag;

import com.fukang.knowledge.agent.domain.rag.model.SearchResult;
import com.huaban.analysis.jieba.JiebaSegmenter;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.aggregator.DefaultContentAggregator;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 多因子特征融合重排序服务
 *
 * <h2>评分模型概述</h2>
 * <p>基于五个独立评分因子进行加权融合，替代旧版简单关键词计数方案。</p>
 *
 * <h3>评分因子</h3>
 * <table>
 *   <tr><th>因子</th><th>权重</th><th>说明</th></tr>
 *   <tr><td>语义相似度 (Semantic Similarity)</td><td>0.50</td><td>直接使用 pgvector 返回的原始余弦相似度分数</td></tr>
 *   <tr><td>词条覆盖率 (Term Coverage)</td><td>0.15</td><td>查询词条在文档中的出现比例</td></tr>
 *   <tr><td>精确短语匹配 (Phrase Match)</td><td>0.15</td><td>查询短语在文档中的连续匹配程度</td></tr>
 *   <tr><td>IDF 加权匹配 (IDF-Weighted)</td><td>0.15</td><td>基于候选文档集合的词条稀有度加权匹配</td></tr>
 *   <tr><td>位置加权匹配 (Position-Weighted)</td><td>0.05</td><td>词条在文档中出现位置越靠前得分越高</td></tr>
 * </table>
 *
 * <h3>融合公式</h3>
 * <pre>
 * finalScore = 0.50 * semanticSimilarity
 *            + 0.15 * termCoverage
 *            + 0.15 * phraseMatch
 *            + 0.15 * idfWeightedMatch
 *            + 0.05 * positionWeightedMatch
 * </pre>
 *
 * <h3>中文停用词</h3>
 * <p>内置常见中文停用词表，在提取查询词条时自动过滤，避免无意义词汇干扰评分。</p>
 */
@Slf4j
@Component
public class RerankService {

    private static final double WEIGHT_SEMANTIC = 0.50;
    private static final double WEIGHT_COVERAGE = 0.15;
    private static final double WEIGHT_PHRASE = 0.15;
    private static final double WEIGHT_IDF = 0.15;
    private static final double WEIGHT_POSITION = 0.05;

    private static final Set<String> CHINESE_STOP_WORDS = Set.of(
            "的", "了", "在", "是", "我", "有", "和", "就", "不", "人",
            "都", "一", "一个", "上", "也", "很", "到", "说", "要", "去",
            "你", "会", "着", "没有", "看", "好", "自己", "这"
    );

    private static final int MIN_TERM_LENGTH = 2;

    private static final JiebaSegmenter SEGMENTER = new JiebaSegmenter();

    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\\u4e00-\\u9fff]");

    /**
     * 多因子融合评分内部结果记录
     *
     * @param candidate        原始检索结果
     * @param finalScore        融合后的最终分数
     * @param termCoverage      词条覆盖率子分数
     * @param phraseMatch       精确短语匹配子分数
     * @param idfScore          IDF 加权匹配子分数
     * @param positionScore     位置加权匹配子分数
     */
    private record RerankResult(
            SearchResult candidate,
            double finalScore,
            double termCoverage,
            double phraseMatch,
            double idfScore,
            double positionScore
    ) {
    }

    /**
     * 构建 RAG 检索增强器
     * <p>由于当前环境未配置 ScoringModel，使用 DefaultContentAggregator 作为降级方案。
     * 实际重排序逻辑在 {@link #rerank(List, String)} 方法中手动执行。</p>
     *
     * @param contentRetriever 内容检索器
     * @param minScore         最低相似度阈值（当前未生效于 aggregator 层）
     * @return 配置好的 RetrievalAugmentor 实例
     */
    public RetrievalAugmentor buildReRankingAugmentor(ContentRetriever contentRetriever, Double minScore) {
        log.info("构建 RAG 增强器: minScore={}, contentAggregator=DefaultContentAggregator (无 ScoringModel 降级)", minScore);
        return DefaultRetrievalAugmentor.builder()
                .contentRetriever(contentRetriever)
                .contentAggregator(new DefaultContentAggregator())
                .build();
    }

    /**
     * 对候选检索结果执行多因子特征融合重排序
     *
     * <p>处理流程：</p>
     * <ol>
     *   <li>预处理：提取查询词条并过滤停用词</li>
     *   <li>计算 IDF：基于候选文档集合计算每个查询词条的逆向文档频率</li>
     *   <li>多因子评分：对每个候选项计算五个因子的子分数并融合</li>
     *   <li>排序输出：按融合分数降序排列，返回重排序后的结果列表</li>
     * </ol>
     *
     * @param candidates 候选检索结果列表
     * @param query      用户原始查询文本
     * @return 按融合分数降序排列的结果列表，输入为空时返回空列表
     */
    public List<SearchResult> rerank(List<SearchResult> candidates, String query) {
        if (candidates == null || candidates.isEmpty() || query == null || query.isBlank()) {
            return candidates != null ? candidates : List.of();
        }

        long startTime = System.currentTimeMillis();

        List<String> queryTerms = extractQueryTerms(query);
        if (queryTerms.isEmpty()) {
            return new ArrayList<>(candidates);
        }

        if (log.isDebugEnabled()) {
            logTop3Before(candidates, query);
        }

        Map<String, Double> idfMap = computeIDF(candidates, queryTerms);

        List<RerankResult> scoredResults = new ArrayList<>();
        for (SearchResult candidate : candidates) {
            String text = candidate.chunkText() != null ? candidate.chunkText().toLowerCase() : "";

            double termCoverage = computeTermCoverage(queryTerms, text);
            double phraseMatch = computePhraseMatch(query, text);
            double idfScore = computeIDFScore(queryTerms, text, idfMap);
            double positionScore = computePositionScore(queryTerms, text);
            double semanticScore = candidate.similarity();

            double finalScore = WEIGHT_SEMANTIC * semanticScore
                    + WEIGHT_COVERAGE * termCoverage
                    + WEIGHT_PHRASE * phraseMatch
                    + WEIGHT_IDF * idfScore
                    + WEIGHT_POSITION * positionScore;

            scoredResults.add(new RerankResult(candidate, finalScore,
                    termCoverage, phraseMatch, idfScore, positionScore));
        }

        scoredResults.sort(Comparator.comparingDouble(RerankResult::finalScore).reversed());

        List<SearchResult> result = scoredResults.stream()
                .map(r -> new SearchResult(
                        r.candidate().chunkId(),
                        r.candidate().chunkText(),
                        r.finalScore(),
                        r.candidate().metadata()
                ))
                .collect(Collectors.toList());

        long elapsed = System.currentTimeMillis() - startTime;

        if (log.isDebugEnabled()) {
            logTop3After(result, query, elapsed);
            log.debug("多因子重排序完成: candidates={}, queryTerms={}, elapsedMs={}",
                    candidates.size(), queryTerms.size(), elapsed);
        }

        return result;
    }

    /**
     * 从查询文本中提取词条列表
     *
     * <p>处理步骤：转小写 → 按空格和标点分割 → 过滤长度不足的 token → 过滤中文停用词 → 去重</p>
     *
     * @param query 原始查询文本
     * @return 去重后的有效词条列表
     */
    private List<String> extractQueryTerms(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String[] tokens = query.toLowerCase().split("[\\s,，。.!！?？;；:：]+");
        return Arrays.stream(tokens)
                .map(String::trim)
                .filter(token -> !token.isEmpty() && token.length() >= MIN_TERM_LENGTH)
                .filter(token -> !CHINESE_STOP_WORDS.contains(token))
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 计算查询词条在候选文档集合中的逆向文档频率（IDF）
     *
     * <p>使用 BM25 IDF 变体公式：</p>
     * <pre>idf = log(1 + (N - df + 0.5) / (df + 0.5))</pre>
     * <p>其中 N 为候选文档总数，df 为包含该词条的文档数。</p>
     * <p>稀有词（仅在少数文档中出现）的 IDF 值更高，在匹配时贡献更大。</p>
     *
     * @param candidates 候选文档列表
     * @param queryTerms 查询词条列表
     * @return 词条到 IDF 值的映射
     */
    private Map<String, Double> computeIDF(List<SearchResult> candidates, List<String> queryTerms) {
        int N = candidates.size();
        Map<String, Double> idfMap = new LinkedHashMap<>();

        for (String term : queryTerms) {
            int df = 0;
            for (SearchResult candidate : candidates) {
                String text = candidate.chunkText() != null ? candidate.chunkText().toLowerCase() : "";
                if (text.contains(term)) {
                    df++;
                }
            }
            double idf = Math.log(1.0 + (N - df + 0.5) / (df + 0.5));
            idfMap.put(term, idf);
        }

        return idfMap;
    }

    /**
     * 计算词条覆盖率（Term Coverage）
     *
     * <p>衡量查询中有多少比例的独立词条出现在了文档中。</p>
     * <pre>coverage = matchedTermsCount / totalQueryTermsCount</pre>
     *
     * @param queryTerms 查询词条列表
     * @param text       文档文本（已转小写）
     * @return 覆盖率分数，范围 [0.0, 1.0]
     */
    private double computeTermCoverage(List<String> queryTerms, String text) {
        if (queryTerms.isEmpty()) {
            return 0.0;
        }
        long matched = queryTerms.stream()
                .filter(text::contains)
                .count();
        return (double) matched / queryTerms.size();
    }

    /**
     * 计算精确短语匹配分数（Phrase Match）
     *
     * <p>将查询按空格分割为词序列，在文档中寻找最大连续匹配段。</p>
     * <pre>phraseMatch = maxConsecutiveMatches / totalQueryWords</pre>
     * <p>例如查询 "人工智能发展" 分割为 ["人工智能发展"]，若文档包含此完整短语则得 1.0。</p>
     *
     * @param query 原始查询文本
     * @param text  文档文本（已转小写）
     * @return 短语匹配分数，范围 [0.0, 1.0]
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

    private String[] segmentQuery(String query) {
        if (containsChinese(query)) {
            return SEGMENTER.sentenceProcess(query).toArray(new String[0]);
        }
        return query.split("\\s+");
    }

    private boolean containsChinese(String text) {
        return CHINESE_PATTERN.matcher(text).find();
    }

    /**
     * 计算 IDF 加权匹配分数
     *
     * <p>对匹配到的查询词条按 IDF 值加权求和，再除以所有查询词条的 IDF 总和进行归一化。</p>
     * <pre>idfScore = sum(idf(term) for matched terms) / sum(idf(term) for all query terms)</pre>
     * <p>如果所有查询词条的 IDF 总和为 0，返回 0。</p>
     *
     * @param queryTerms 查询词条列表
     * @param text       文档文本（已转小写）
     * @param idfMap     词条到 IDF 值的映射
     * @return IDF 加权匹配分数，范围 [0.0, 1.0]
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
     * 计算位置加权匹配分数（Position-Weighted Match）
     *
     * <p>词条在文档中越早出现，得分越高。对每个匹配词条计算位置衰减分数后取平均值。</p>
     * <pre>posScore(term) = min(1.0, 1.0 / (1.0 + firstPosition / 100.0))</pre>
     * <p>例如位置 0 得 1.0，位置 100 得 0.5，位置 900 得 0.1。</p>
     *
     * @param queryTerms 查询词条列表
     * @param text       文档文本（已转小写）
     * @return 平均位置加权分数，范围 [0.0, 1.0]
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
                double posScore = Math.min(1.0, 1.0 / (1.0 + firstPos / 100.0));
                totalPositionScore += posScore;
                matchedCount++;
            }
        }

        if (matchedCount == 0) {
            return 0.0;
        }
        return totalPositionScore / matchedCount;
    }

    private void logTop3Before(List<SearchResult> candidates, String query) {
        StringBuilder sb = new StringBuilder();
        sb.append("重排序前 Top-3:\n");
        int limit = Math.min(3, candidates.size());
        for (int i = 0; i < limit; i++) {
            SearchResult r = candidates.get(i);
            sb.append(String.format("  [%d] similarity=%.4f, chunkId=%d\n",
                    i + 1, r.similarity(), r.chunkId()));
        }
        log.debug("查询=[{}], {}", query, sb);
    }

    private void logTop3After(List<SearchResult> results, String query, long elapsedMs) {
        StringBuilder sb = new StringBuilder();
        sb.append("重排序后 Top-3 (elapsed=").append(elapsedMs).append("ms):\n");
        int limit = Math.min(3, results.size());
        for (int i = 0; i < limit; i++) {
            SearchResult r = results.get(i);
            sb.append(String.format("  [%d] similarity=%.4f, chunkId=%d\n",
                    i + 1, r.similarity(), r.chunkId()));
        }
        log.debug("查询=[{}], {}", query, sb);
    }
}