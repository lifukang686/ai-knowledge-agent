package com.fukang.knowledge.agent.application.rag.intent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fukang.knowledge.agent.application.ai.port.ChatCompletionPort;
import com.fukang.knowledge.agent.infrastructure.ai.PromptTemplateManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * LLM fallback intent classifier.
 * <p>Used only when rules cannot confidently classify the question.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LlmQaIntentClassifier {

    private static final String INTENT_TEMPLATE = "rag/intent-classifier.v1";

    private final ChatCompletionPort chatCompletionPort;
    private final PromptTemplateManager promptTemplateManager;
    private final ObjectMapper objectMapper;

    public QaIntentResult classifyResult(String question) {
        try {
            String userPrompt = promptTemplateManager.renderText(INTENT_TEMPLATE, Map.of(
                    "question", question != null ? question : ""
            ));
            String raw = chatCompletionPort.complete(List.of(
                    ChatCompletionPort.Message.system("你是一个严格的问答意图分类器，只输出 JSON。"),
                    ChatCompletionPort.Message.user(userPrompt)
            ));
            return parse(raw);
        } catch (Exception e) {
            log.warn("LLM intent classification failed, fallback to RAG_QA: {}", e.getMessage());
            return QaIntentResult.of(QaIntent.RAG_QA, 0.3, "llm classifier failed");
        }
    }

    private QaIntentResult parse(String raw) throws Exception {
        if (raw == null || raw.isBlank()) {
            return QaIntentResult.of(QaIntent.RAG_QA, 0.3, "empty llm response");
        }
        JsonNode root = objectMapper.readTree(extractJson(raw));
        QaIntent intent = parseIntent(root.path("intent").asText("RAG_QA"));
        double confidence = root.path("confidence").asDouble(0.5);
        String reason = root.path("reason").asText("llm classified");
        return QaIntentResult.of(intent, clamp(confidence), reason);
    }

    private QaIntent parseIntent(String value) {
        try {
            return QaIntent.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return QaIntent.RAG_QA;
        }
    }

    private String extractJson(String raw) {
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return raw.substring(start, end + 1);
        }
        return raw;
    }

    private double clamp(double confidence) {
        if (confidence < 0) {
            return 0;
        }
        if (confidence > 1) {
            return 1;
        }
        return confidence;
    }
}
