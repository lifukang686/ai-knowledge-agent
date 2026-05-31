package com.fukang.knowledge.agent.application.rag.intent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Two-stage QA intent classifier.
 * <p>Rules run first; LLM fallback runs only when rules return UNKNOWN.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HybridQaIntentClassifier implements QaIntentClassifier {

    private final RuleBasedQaIntentClassifier ruleBasedQaIntentClassifier;
    private final LlmQaIntentClassifier llmQaIntentClassifier;

    @Override
    public QaIntentResult classifyResult(String question) {
        QaIntentResult ruleResult = ruleBasedQaIntentClassifier.classifyResult(question);
        if (ruleResult.intent() != QaIntent.UNKNOWN) {
            return ruleResult;
        }

        QaIntentResult llmResult = llmQaIntentClassifier.classifyResult(question);
        log.debug("LLM intent fallback result: intent={}, confidence={}, reason={}",
                llmResult.intent(), llmResult.confidence(), llmResult.reason());
        return llmResult;
    }
}
