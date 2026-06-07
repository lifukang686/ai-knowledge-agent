package com.fukang.knowledge.agent.application.rag.intent;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 规则意图分类器。
 * <p>仅处理高置信度的直聊、记忆和知识问答场景。</p>
 */
@Component
public class RuleBasedQaIntentClassifier {

    /** 用户主动提供身份、称呼等个人信息。 */
    private static final Pattern SELF_DISCLOSURE_PATTERN = Pattern.compile(
            "(我是|我叫|我的名字是|本人是|以后叫我|你可以叫我|请叫我).{1,40}",
            Pattern.CASE_INSENSITIVE
    );

    /** 用户询问已记住的身份信息。 */
    private static final Pattern USER_IDENTITY_QUESTION_PATTERN = Pattern.compile(
            "(我是谁|我叫什么|我的名字是什么|你知道我是谁吗|你还记得我是谁吗|还记得我是谁吗|你知道我叫什么吗)",
            Pattern.CASE_INSENSITIVE
    );

    /** 用户询问助手身份或能力。 */
    private static final Pattern ASSISTANT_IDENTITY_PATTERN = Pattern.compile(
            "(你是|你是谁|你叫什么|你叫啥|你呢|介绍一下你|自我介绍|你能做什么|你有什么功能|你的能力)",
            Pattern.CASE_INSENSITIVE
    );

    /** 常见业务制度类提问特征。 */
    private static final Pattern KNOWLEDGE_QUESTION_PATTERN = Pattern.compile(
            "(啥等级|什么等级|等级|绩效|分数|得了\\d+分|扣了|规则|制度|标准|怎么算|如何计算|属于)",
            Pattern.CASE_INSENSITIVE
    );

    /** 自然语言疑问特征，命中后避免误判为纯记忆。 */
    private static final Pattern QUESTION_SIGNAL_PATTERN = Pattern.compile(
            "(\\?|？|请问|吗|么|呢|什么|啥|怎么|如何|怎样|为什么|多少|几个|哪|是否|能不能|可以不可以)",
            Pattern.CASE_INSENSITIVE
    );

    /** 明确不需要检索知识库的闲聊。 */
    private static final Pattern DIRECT_CHAT_PATTERN = Pattern.compile(
            "(你好|您好|哈喽|hello|hi|早上好|下午好|晚上好|晚安|再见|拜拜|谢谢|感谢|辛苦了"
                    + "|讲个笑话|说个笑话|讲个故事|说个故事|现在几点|今天几号|星期几|周几)",
            Pattern.CASE_INSENSITIVE
    );

    /** 明确指向文档、项目、知识库的提问特征。 */
    private static final Pattern EXPLICIT_KNOWLEDGE_PATTERN = Pattern.compile(
            "(文档|知识库|资料|手册|项目|代码|接口|表结构|配置|流程|怎么实现|如何实现|原因|区别|步骤|方案)",
            Pattern.CASE_INSENSITIVE
    );

    /** 按规则识别问答意图。 */
    public QaIntentResult classifyResult(String question) {
        if (question == null || question.isBlank()) {
            return QaIntentResult.of(QaIntent.UNKNOWN, 0.0, "空问题");
        }

        String normalized = question.trim().replaceAll("\\s+", "");
        if (USER_IDENTITY_QUESTION_PATTERN.matcher(normalized).find()) {
            return QaIntentResult.of(QaIntent.DIRECT_CHAT, 0.98, "询问已记住的用户身份");
        }

        boolean selfDisclosure = SELF_DISCLOSURE_PATTERN.matcher(normalized).find();
        boolean assistantIdentity = ASSISTANT_IDENTITY_PATTERN.matcher(normalized).find();
        boolean knowledgeQuestion = KNOWLEDGE_QUESTION_PATTERN.matcher(normalized).find()
                || EXPLICIT_KNOWLEDGE_PATTERN.matcher(normalized).find();
        boolean questionSignal = QUESTION_SIGNAL_PATTERN.matcher(normalized).find();

        if (knowledgeQuestion) {
            return QaIntentResult.of(QaIntent.RAG_QA, 0.80, "命中知识提问特征");
        }

        if (selfDisclosure && assistantIdentity) {
            return QaIntentResult.of(QaIntent.MEMORY_CHAT, 0.98, "提供个人信息并询问助手");
        }
        if (selfDisclosure && questionSignal) {
            return QaIntentResult.of(QaIntent.UNKNOWN, 0.0, "自述中包含疑问，交由模型判断");
        }
        if (selfDisclosure) {
            return QaIntentResult.of(QaIntent.MEMORY_UPDATE, 0.96, "提供个人信息");
        }
        if (assistantIdentity || DIRECT_CHAT_PATTERN.matcher(normalized).find()) {
            return QaIntentResult.of(QaIntent.DIRECT_CHAT, 0.95, "命中闲聊特征");
        }
        return QaIntentResult.of(QaIntent.UNKNOWN, 0.0, "未命中规则");
    }
}
