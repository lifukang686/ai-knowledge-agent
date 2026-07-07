package com.fukang.knowledge.agent.module.rag.service.intent.impl;

import com.fukang.knowledge.agent.module.rag.service.intent.QaIntent;
import com.fukang.knowledge.agent.module.rag.service.intent.QaIntentClassifier;
import com.fukang.knowledge.agent.module.rag.service.intent.QaIntentResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 两阶段 QA 意图分类器。
 * <p>先执行高置信度规则，规则无法判断时再调用 LLM 兜底。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HybridQaIntentClassifier implements QaIntentClassifier {

    private final RuleBasedQaIntentClassifier ruleBasedQaIntentClassifier;
    private final LlmQaIntentClassifier llmQaIntentClassifier;

    /**
     * 先规则识别，规则无法判断时再调用 LLM。
     */
    @Override
    public QaIntentResult classifyResult(String question) {
        QaIntentResult ruleResult = ruleBasedQaIntentClassifier.classifyResult(question);
        if (ruleResult.getIntent() != QaIntent.UNKNOWN) {
            return ruleResult;
        }

        QaIntentResult llmResult = llmQaIntentClassifier.classifyResult(question);
        log.debug("LLM intent fallback result: intent={}, confidence={}, reason={}",
                llmResult.getIntent(), llmResult.getConfidence(), llmResult.getReason());
        return llmResult;
    }
}
