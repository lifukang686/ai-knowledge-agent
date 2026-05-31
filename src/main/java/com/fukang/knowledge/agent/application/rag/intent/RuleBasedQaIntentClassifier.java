package com.fukang.knowledge.agent.application.rag.intent;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Rule-based intent classifier.
 * <p>It only returns high-confidence results for obvious non-RAG or RAG questions.</p>
 */
@Component
public class RuleBasedQaIntentClassifier {

    private static final Pattern SELF_DISCLOSURE_PATTERN = Pattern.compile(
            "(我是|我叫|我的名字是|本人是|以后叫我|你可以叫我|请叫我).{1,40}",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern USER_IDENTITY_QUESTION_PATTERN = Pattern.compile(
            "(我是谁|我叫什么|我的名字是什么|你知道我是谁吗|你还记得我是谁吗|还记得我是谁吗|你知道我叫什么吗)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern ASSISTANT_IDENTITY_PATTERN = Pattern.compile(
            "(你是|你是谁|你叫什么|你叫啥|你呢|介绍一下你|自我介绍|你能做什么|你有什么功能|你的能力)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern DIRECT_CHAT_PATTERN = Pattern.compile(
            "(你好|您好|哈喽|hello|hi|早上好|下午好|晚上好|晚安|再见|拜拜|谢谢|感谢|辛苦了"
                    + "|讲个笑话|说个笑话|讲个故事|说个故事|现在几点|今天几号|星期几|周几)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern EXPLICIT_KNOWLEDGE_PATTERN = Pattern.compile(
            "(文档|知识库|资料|手册|项目|代码|接口|表结构|配置|流程|怎么实现|如何实现|原因|区别|步骤|方案)",
            Pattern.CASE_INSENSITIVE
    );

    public QaIntentResult classifyResult(String question) {
        if (question == null || question.isBlank()) {
            return QaIntentResult.of(QaIntent.UNKNOWN, 0.0, "empty question");
        }

        String normalized = question.trim().replaceAll("\\s+", "");
        if (USER_IDENTITY_QUESTION_PATTERN.matcher(normalized).find()) {
            return QaIntentResult.of(QaIntent.DIRECT_CHAT, 0.98, "user asks about remembered identity");
        }

        boolean selfDisclosure = SELF_DISCLOSURE_PATTERN.matcher(normalized).find();
        boolean assistantIdentity = ASSISTANT_IDENTITY_PATTERN.matcher(normalized).find();

        if (selfDisclosure && assistantIdentity) {
            return QaIntentResult.of(QaIntent.MEMORY_CHAT, 0.98, "self disclosure with assistant identity question");
        }
        if (selfDisclosure) {
            return QaIntentResult.of(QaIntent.MEMORY_UPDATE, 0.96, "self disclosure");
        }
        if (assistantIdentity || DIRECT_CHAT_PATTERN.matcher(normalized).find()) {
            return QaIntentResult.of(QaIntent.DIRECT_CHAT, 0.95, "direct chat pattern");
        }
        if (EXPLICIT_KNOWLEDGE_PATTERN.matcher(normalized).find()) {
            return QaIntentResult.of(QaIntent.RAG_QA, 0.80, "explicit knowledge question pattern");
        }
        return QaIntentResult.of(QaIntent.UNKNOWN, 0.0, "no rule matched");
    }
}
