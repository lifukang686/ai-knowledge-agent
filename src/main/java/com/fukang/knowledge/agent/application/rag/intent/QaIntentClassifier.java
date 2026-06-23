package com.fukang.knowledge.agent.application.rag.intent;

/**
 * QA 意图识别器。
 */
public interface QaIntentClassifier {

    /**
     * 返回完整意图识别结果。
     */
    QaIntentResult classifyResult(String question);

    /**
     * 返回意图枚举。
     */
    default QaIntent classify(String question) {
        return classifyResult(question).intent();
    }
}
