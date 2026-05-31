package com.fukang.knowledge.agent.application.rag.intent;

/**
 * QA 意图识别器。
 */
public interface QaIntentClassifier {

    QaIntentResult classifyResult(String question);

    default QaIntent classify(String question) {
        return classifyResult(question).intent();
    }
}
