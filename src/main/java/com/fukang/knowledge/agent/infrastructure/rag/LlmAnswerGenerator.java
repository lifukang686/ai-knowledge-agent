package com.fukang.knowledge.agent.infrastructure.rag;

import com.fukang.knowledge.agent.common.enums.ModelTypeEnum;
import com.fukang.knowledge.agent.domain.rag.model.SearchResult;
import com.fukang.knowledge.agent.domain.rag.service.AnswerGenerator;
import com.fukang.knowledge.agent.infrastructure.ai.DynamicModelManager;
import com.fukang.knowledge.agent.infrastructure.ai.PromptTemplateManager;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmAnswerGenerator implements AnswerGenerator {

    private static final String ANSWER_SYSTEM_TEMPLATE = "rag/answer-system.v1";
    private static final String NOT_FOUND_MESSAGE = "抱歉，未找到与您问题相关的文档内容。";

    private final DynamicModelManager dynamicModelManager;
    private final PromptTemplateManager promptTemplateManager;

    @Override
    public String generateAnswer(List<SearchResult> results, String query) {
        if (results.isEmpty()) {
            return NOT_FOUND_MESSAGE;
        }

        String userPrompt = buildUserPrompt(results, query);
        try {
            ChatLanguageModel chatModel = dynamicModelManager.getChatModel(ModelTypeEnum.CHAT);
            List<ChatMessage> messages = List.of(
                    promptTemplateManager.renderSystem(ANSWER_SYSTEM_TEMPLATE, null),
                    UserMessage.from(userPrompt)
            );
            Response<AiMessage> response = chatModel.generate(messages);
            String answer = response.content().text();
            if (answer == null || answer.isBlank()) {
                log.warn("LLM 返回空回答");
                return NOT_FOUND_MESSAGE;
            }
            return answer;
        } catch (Exception e) {
            log.error("LLM 生成回答失败，降级为文本拼接", e);
            return formatFallbackAnswer(results);
        }
    }

    String buildUserPrompt(List<SearchResult> results, String query) {
        return buildRagUserPrompt(results, query);
    }

    /**
     * 构造 RAG 回答的用户消息，供同步和流式生成复用。
     */
    public String buildRagUserPrompt(List<SearchResult> results, String query) {
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            SearchResult result = results.get(i);
            context.append("【文档片段").append(i + 1).append("】")
                    .append(result.chunkText())
                    .append("\n");
        }
        return String.format("【文档内容】%n%s%n【用户问题】%n%s", context, query);
    }

    private String formatFallbackAnswer(List<SearchResult> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("为您找到以下相关内容：\n\n");
        for (int i = 0; i < results.size(); i++) {
            SearchResult result = results.get(i);
            sb.append("【").append(i + 1).append("】").append(result.chunkText()).append("\n");
            sb.append("（相关度：").append(String.format("%.2f", result.similarity())).append("）\n\n");
        }
        return sb.toString().trim();
    }
}
