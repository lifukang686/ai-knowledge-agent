package com.fukang.knowledge.agent.application.rag;

import com.fukang.knowledge.agent.application.ai.port.ChatCompletionPort;
import com.fukang.knowledge.agent.application.ai.port.StreamingChatCompletionPort;
import com.fukang.knowledge.agent.application.conversation.ConversationMemoryContext;
import com.fukang.knowledge.agent.application.conversation.ConversationMemoryService;
import com.fukang.knowledge.agent.application.knowledge.port.KnowledgeBaseRepository;
import com.fukang.knowledge.agent.application.rag.intent.QaIntent;
import com.fukang.knowledge.agent.application.rag.intent.QaIntentClassifier;
import com.fukang.knowledge.agent.application.rag.result.QaResult;
import com.fukang.knowledge.agent.application.rag.stream.QaStreamHandler;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.domain.rag.model.SearchResult;
import com.fukang.knowledge.agent.domain.rag.service.AnswerGenerator;
import com.fukang.knowledge.agent.domain.rag.service.QueryRewritePort;
import com.fukang.knowledge.agent.domain.rag.service.Reranker;
import com.fukang.knowledge.agent.domain.rag.service.RetrievalStrategy;
import com.fukang.knowledge.agent.infrastructure.ai.PromptTemplateManager;
import com.fukang.knowledge.agent.infrastructure.config.RetrievalProperties;
import com.fukang.knowledge.agent.infrastructure.rag.LlmAnswerGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagAppService {

    private static final String CHITCHAT_SYSTEM_TEMPLATE = "rag/chitchat-system.v1";

    private static final Pattern CHITCHAT_PATTERN = Pattern.compile(
            "(你好|您好|哈喽|hello|hi|早上好|下午好|晚上好|晚安|再见|拜拜|谢谢|感谢|辛苦了"
                    + "|(你是谁|你是|你叫什么|你能|你可以|你会|功能|能力)"
                    + "|自我介绍|介绍一下"
                    + "|(讲个|说个|来个)?(笑话|故事)"
                    + "|(天气|几点|今天(几号|星期几|周几)|现在几点|时间)"
                    + "|(帮我)?(翻译|算一下|计算))",
            Pattern.CASE_INSENSITIVE
    );

    private final QueryRewritePort queryRewritePort;
    private final RetrievalStrategy retrievalStrategy;
    private final Reranker reranker;
    private final AnswerGenerator answerGenerator;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final RetrievalProperties retrievalProperties;
    private final PromptTemplateManager promptTemplateManager;
    private final ChatCompletionPort chatCompletionPort;
    private final StreamingChatCompletionPort streamingChatCompletionPort;
    private final LlmAnswerGenerator llmAnswerGenerator;
    private final ConversationMemoryService conversationMemoryService;
    private final QaIntentClassifier qaIntentClassifier;

    public QaResult answer(String question, Long knowledgeBaseId, Long conversationId) {
        validateKnowledgeBase(knowledgeBaseId);
        ConversationMemoryContext memory = conversationMemoryService.prepareContext(conversationId, knowledgeBaseId, question);
        QaIntent intent = qaIntentClassifier.classify(question);

        if (shouldBypassRetrieval(question, intent)) {
            log.info("Bypass RAG retrieval: intent={}, question={}", intent, question);
            String answer = directByIntent(question, memory, intent);
            saveSuccessfulTurn(memory.conversationId(), question, question, answer);
            return new QaResult(answer, question, "success", memory.conversationId());
        }

        String rewrittenQuery = queryRewritePort.rewriteWithHistory(
                question, memory.summary(), memory.rewriteHistory());
        List<SearchResult> retrieved = retrieveWithFallback(rewrittenQuery, question, knowledgeBaseId);

        if (retrieved.isEmpty() && isChitchat(rewrittenQuery)) {
            log.info("Fallback to direct chat because retrieval is empty and rewritten query is chitchat");
            String answer = directChat(rewrittenQuery, memory);
            saveSuccessfulTurn(memory.conversationId(), question, rewrittenQuery, answer);
            return new QaResult(answer, rewrittenQuery, "success", memory.conversationId());
        }

        List<SearchResult> reranked = reranker.rerank(retrieved, question);
        String status = reranked.isEmpty() ? "no_results" : "success";
        String answer = answerGenerator.generateAnswer(reranked, rewrittenQuery, buildAnswerMemory(memory));
        conversationMemoryService.saveUserMessage(memory.conversationId(), question, rewrittenQuery, status);
        conversationMemoryService.saveAssistantMessage(memory.conversationId(), answer, status);
        return new QaResult(answer, rewrittenQuery, status, memory.conversationId());
    }

    /**
     * Stream RAG answer with stage events and token events.
     */
    public void answerStream(String question, Long knowledgeBaseId, Long conversationId, QaStreamHandler handler) {
        try {
            validateKnowledgeBase(knowledgeBaseId);
            ConversationMemoryContext memory = conversationMemoryService.prepareContext(conversationId, knowledgeBaseId, question);
            QaIntent intent = qaIntentClassifier.classify(question);

            if (shouldBypassRetrieval(question, intent)) {
                handler.onStage("generate_start", "检测到非知识库问题，正在直接回答");
                streamDirectByIntent(question, memory, intent, handler);
                return;
            }

            handler.onStage("rewrite_start", "正在结合会话历史改写查询");
            String rewrittenQuery = queryRewritePort.rewriteWithHistory(
                    question, memory.summary(), memory.rewriteHistory());
            handler.onStage("rewrite_done", "查询改写完成");

            handler.onStage("retrieve_start", "正在检索知识库");
            List<SearchResult> retrieved = retrieveWithFallback(rewrittenQuery, question, knowledgeBaseId);
            handler.onStage("retrieve_done", "检索完成，找到 " + retrieved.size() + " 个候选片段");

            if (retrieved.isEmpty() && isChitchat(rewrittenQuery)) {
                handler.onStage("generate_start", "检索无结果，降级为直接回答");
                streamDirectChat(rewrittenQuery, memory, handler);
                return;
            }

            handler.onStage("rerank_start", "正在对检索结果重排序");
            List<SearchResult> reranked = reranker.rerank(retrieved, question);
            handler.onStage("rerank_done", "重排序完成，保留 " + reranked.size() + " 个候选片段");

            if (reranked.isEmpty()) {
                String answer = "抱歉，未找到与您问题相关的文档内容。";
                conversationMemoryService.saveUserMessage(memory.conversationId(), question, rewrittenQuery, "no_results");
                conversationMemoryService.saveAssistantMessage(memory.conversationId(), answer, "no_results");
                handler.onToken(answer);
                handler.onDone(new QaResult(answer, rewrittenQuery, "no_results", memory.conversationId()));
                return;
            }

            handler.onStage("generate_start", "正在生成回答");
            String systemPrompt = promptTemplateManager.renderText("rag/answer-system.v1", null);
            String userPrompt = llmAnswerGenerator.buildRagUserPrompt(reranked, rewrittenQuery, buildAnswerMemory(memory));
            streamChat(List.of(
                    ChatCompletionPort.Message.system(systemPrompt),
                    ChatCompletionPort.Message.user(userPrompt)
            ), question, rewrittenQuery, "success", memory.conversationId(), handler);
        } catch (Exception e) {
            log.error("Stream RAG answer failed", e);
            handler.onError("生成失败，请稍后重试", e);
        }
    }

    private boolean shouldBypassRetrieval(String question, QaIntent intent) {
        return intent.bypassRetrieval() || isChitchat(question);
    }

    private void validateKnowledgeBase(Long knowledgeBaseId) {
        if (knowledgeBaseId != null && knowledgeBaseRepository.findById(knowledgeBaseId) == null) {
            log.warn("Knowledge base does not exist: id={}", knowledgeBaseId);
            throw new BaseException(ErrorCodeEnum.KNOWLEDGE_BASE_NOT_EXIST);
        }
    }

    private boolean isChitchat(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return text.trim().length() <= 30 && CHITCHAT_PATTERN.matcher(text).find();
    }

    private String directByIntent(String question, ConversationMemoryContext memory, QaIntent intent) {
        if (intent == QaIntent.MEMORY_UPDATE) {
            return "好的，我记住了。";
        }
        return directChat(question, memory);
    }

    private String directChat(String question, ConversationMemoryContext memory) {
        try {
            String answer = chatCompletionPort.complete(List.of(
                    ChatCompletionPort.Message.system(promptTemplateManager.renderText(CHITCHAT_SYSTEM_TEMPLATE, null)),
                    ChatCompletionPort.Message.user(buildDirectChatPrompt(question, memory))
            ));
            return answer == null || answer.isBlank()
                    ? "你好！我是你的知识库问答助手，可以帮你基于已上传文档进行问答。"
                    : answer;
        } catch (Exception e) {
            log.error("Direct LLM answer failed", e);
            return "你好！我是你的知识库问答助手，可以帮你基于已上传文档进行问答。";
        }
    }

    private void streamDirectByIntent(String question, ConversationMemoryContext memory,
                                      QaIntent intent, QaStreamHandler handler) {
        if (intent == QaIntent.MEMORY_UPDATE) {
            String answer = "好的，我记住了。";
            saveSuccessfulTurn(memory.conversationId(), question, question, answer);
            handler.onToken(answer);
            handler.onDone(new QaResult(answer, question, "success", memory.conversationId()));
            return;
        }
        streamDirectChat(question, memory, handler);
    }

    private void streamDirectChat(String question, ConversationMemoryContext memory, QaStreamHandler handler) {
        streamChat(List.of(
                ChatCompletionPort.Message.system(promptTemplateManager.renderText(CHITCHAT_SYSTEM_TEMPLATE, null)),
                ChatCompletionPort.Message.user(buildDirectChatPrompt(question, memory))
        ), question, question, "success", memory.conversationId(), handler);
    }

    private void streamChat(List<ChatCompletionPort.Message> messages,
                            String originalQuestion,
                            String rewrittenQuery,
                            String status,
                            Long conversationId,
                            QaStreamHandler handler) {
        StringBuilder answer = new StringBuilder();
        streamingChatCompletionPort.completeStream(messages, new StreamingChatCompletionPort.StreamHandler() {
            @Override
            public void onToken(String token) {
                answer.append(token);
                handler.onToken(token);
            }

            @Override
            public void onComplete(String fullText) {
                String finalAnswer = fullText != null && !fullText.isBlank()
                        ? fullText
                        : answer.toString();
                conversationMemoryService.saveUserMessage(conversationId, originalQuestion, rewrittenQuery, status);
                conversationMemoryService.saveAssistantMessage(conversationId, finalAnswer, status);
                handler.onDone(new QaResult(finalAnswer, rewrittenQuery, status, conversationId));
            }

            @Override
            public void onError(Throwable error) {
                conversationMemoryService.saveUserMessage(conversationId, originalQuestion, rewrittenQuery, "failed");
                handler.onError("生成失败，请稍后重试", error);
            }
        });
    }

    private void saveSuccessfulTurn(Long conversationId, String question, String rewrittenQuery, String answer) {
        conversationMemoryService.saveUserMessage(conversationId, question, rewrittenQuery, "success");
        conversationMemoryService.saveAssistantMessage(conversationId, answer, "success");
    }

    private String buildDirectChatPrompt(String question, ConversationMemoryContext memory) {
        String answerMemory = buildAnswerMemory(memory);
        if (answerMemory.isBlank()) {
            return question;
        }
        return "【会话记忆】\n" + answerMemory + "\n\n【用户问题】\n" + question;
    }

    private String buildAnswerMemory(ConversationMemoryContext memory) {
        StringBuilder builder = new StringBuilder();
        if (memory.summary() != null && !memory.summary().isBlank()) {
            builder.append("摘要：").append(memory.summary()).append("\n");
        }
        if (memory.answerHistory() != null && !memory.answerHistory().isBlank()) {
            builder.append("最近对话：\n").append(memory.answerHistory());
        }
        return builder.toString().trim();
    }

    private List<SearchResult> retrieveWithFallback(String rewrittenQuery, String originalQuery, Long knowledgeBaseId) {
        int topK = retrievalProperties.getTopK();
        double threshold = retrievalProperties.getSimilarityThreshold();

        List<SearchResult> rewrittenResults = retrievalStrategy.retrieve(
                rewrittenQuery, knowledgeBaseId, topK, threshold);

        if (rewrittenResults.size() >= topK || rewrittenQuery.equals(originalQuery)) {
            return rewrittenResults;
        }

        log.info("Rewritten query has insufficient results({} < {}), supplementing with original query",
                rewrittenResults.size(), topK);
        List<SearchResult> originalResults = retrievalStrategy.retrieve(
                originalQuery, knowledgeBaseId, topK, threshold);

        Set<Long> existingChunkIds = rewrittenResults.stream()
                .map(SearchResult::chunkId)
                .collect(Collectors.toSet());

        List<SearchResult> allResults = new ArrayList<>(rewrittenResults);
        for (SearchResult result : originalResults) {
            if (!existingChunkIds.contains(result.chunkId())) {
                allResults.add(result);
                existingChunkIds.add(result.chunkId());
            }
        }
        allResults.sort(Comparator.comparingDouble(SearchResult::similarity).reversed());
        if (allResults.size() > topK) {
            allResults = allResults.subList(0, topK);
        }
        return allResults;
    }
}
