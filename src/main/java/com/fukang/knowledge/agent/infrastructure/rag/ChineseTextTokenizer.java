package com.fukang.knowledge.agent.infrastructure.rag;

import com.huaban.analysis.jieba.JiebaSegmenter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.stream.Collectors;

@Component
public class ChineseTextTokenizer {

    private final JiebaSegmenter segmenter = new JiebaSegmenter();

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
