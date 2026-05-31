package com.fukang.knowledge.agent.infrastructure.rag;

import com.huaban.analysis.jieba.JiebaSegmenter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.stream.Collectors;

/**
 * 中文全文检索分词器。
 * <p>PostgreSQL simple 分词器不理解中文边界，这里先用 Jieba 切词，
 * 再把 token 用空格拼接成适合 to_tsvector('simple', ...) 的检索文本。</p>
 */
@Component
public class ChineseTextTokenizer {

    /** Jieba 分词器实例，线程安全需求较低，当前作为轻量组件复用。 */
    private final JiebaSegmenter segmenter = new JiebaSegmenter();

    /**
     * 将中文/中英混合文本转换为空格分隔的检索 token。
     *
     * @param text 原始文本
     * @return 归一化后的检索文本，空输入返回空串
     */
    public String tokenize(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return segmenter.sentenceProcess(text).stream()
                .map(token -> token == null ? "" : token.trim().toLowerCase(Locale.ROOT))
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.joining(" "));
    }
}
