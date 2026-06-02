package com.fukang.knowledge.agent.application.rag;

import com.fukang.knowledge.agent.application.ai.port.ChatCompletionPort;
import com.fukang.knowledge.agent.application.conversation.ConversationMemoryContext;
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
import com.fukang.knowledge.agent.infrastructure.ai.PromptTemplateManager;
import com.fukang.knowledge.agent.infrastructure.rag.LlmAnswerGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

/**
 * RAG 问答编排服务。
 * <p>串联意图识别、查询改写、知识库检索、重排、回答生成和会话记忆保存。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagAppService {

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
    private final RagRetrievalService ragRetrievalService;
    private final Reranker reranker;
    private final AnswerGenerator answerGenerator;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final PromptTemplateManager promptTemplateManager;
    private final LlmAnswerGenerator llmAnswerGenerator;
    private final RagConversationService ragConversationService;
    private final RagDirectAnswerService ragDirectAnswerService;
    private final RagStreamingService ragStreamingService;
    private final QaIntentClassifier qaIntentClassifier;

    /**
     * 非流式 RAG 问答主流程。
     */
    public QaResult answer(String question, Long knowledgeBaseId, Long conversationId) {
        validateKnowledgeBase(knowledgeBaseId);
        ConversationMemoryContext memory = ragConversationService.prepareContext(conversationId, knowledgeBaseId, question);
        QaIntent intent = qaIntentClassifier.classify(question);

        // 闲聊、记忆更新等问题不走知识库检索，直接由通用对话分支处理。
        if (shouldBypassRetrieval(question, intent)) {
            log.info("Bypass RAG retrieval: intent={}, question={}", intent, question);
            return ragDirectAnswerService.answerByIntent(question, memory, intent);
        }

        // 查询改写使用会话摘要和最近历史，提升多轮问题的可检索性。
        String rewrittenQuery = queryRewritePort.rewriteWithHistory(
                question, memory.summary(), memory.rewriteHistory());
        List<SearchResult> retrieved = ragRetrievalService.retrieveWithFallback(rewrittenQuery, question, knowledgeBaseId);

        // 改写后被识别为闲聊且无召回时，降级为直接对话，避免返回生硬的无结果。
        if (retrieved.isEmpty() && isChitchat(rewrittenQuery)) {
            log.info("Fallback to direct chat because retrieval is empty and rewritten query is chitchat");
            return ragDirectAnswerService.answerDirectChat(rewrittenQuery, question, rewrittenQuery, memory);
        }

        List<SearchResult> reranked = reranker.rerank(retrieved, question);
        String status = reranked.isEmpty() ? "no_results" : "success";
        String answer = answerGenerator.generateAnswer(reranked, rewrittenQuery,
                ragConversationService.buildAnswerMemory(memory));
        ragConversationService.saveTurn(memory.conversationId(), question, rewrittenQuery, answer, status);
        return new QaResult(answer, rewrittenQuery, status, memory.conversationId());
    }

    /**
     * 流式 RAG 问答，向前端发送阶段事件和 token 事件。
     */
    public void answerStream(String question, Long knowledgeBaseId, Long conversationId, QaStreamHandler handler) {
        try {
            validateKnowledgeBase(knowledgeBaseId);
            ConversationMemoryContext memory = ragConversationService.prepareContext(conversationId, knowledgeBaseId, question);
            QaIntent intent = qaIntentClassifier.classify(question);

            if (shouldBypassRetrieval(question, intent)) {
                handler.onStage("generate_start", "检测到非知识库问题，正在直接回答");
                ragDirectAnswerService.streamByIntent(question, memory, intent, handler);
                return;
            }

            handler.onStage("rewrite_start", "正在结合会话历史改写查询");
            String rewrittenQuery = queryRewritePort.rewriteWithHistory(
                    question, memory.summary(), memory.rewriteHistory());
            handler.onStage("rewrite_done", "查询改写完成");

            handler.onStage("retrieve_start", "正在检索知识库");
            List<SearchResult> retrieved = ragRetrievalService.retrieveWithFallback(rewrittenQuery, question, knowledgeBaseId);
            handler.onStage("retrieve_done", "检索完成，找到 " + retrieved.size() + " 个候选片段");

            if (retrieved.isEmpty() && isChitchat(rewrittenQuery)) {
                handler.onStage("generate_start", "检索无结果，降级为直接回答");
                ragDirectAnswerService.streamDirectChat(rewrittenQuery, rewrittenQuery, rewrittenQuery, memory, handler);
                return;
            }

            handler.onStage("rerank_start", "正在对检索结果重排序");
            List<SearchResult> reranked = reranker.rerank(retrieved, question);
            handler.onStage("rerank_done", "重排序完成，保留 " + reranked.size() + " 个候选片段");

            if (reranked.isEmpty()) {
                String answer = "抱歉，未找到与您问题相关的文档内容。";
                ragConversationService.saveTurn(memory.conversationId(), question, rewrittenQuery, answer, "no_results");
                handler.onToken(answer);
                handler.onDone(new QaResult(answer, rewrittenQuery, "no_results", memory.conversationId()));
                return;
            }

            handler.onStage("generate_start", "正在生成回答");
            String systemPrompt = promptTemplateManager.renderText("rag/answer-system.v1", null);
            String userPrompt = llmAnswerGenerator.buildRagUserPrompt(reranked, rewrittenQuery,
                    ragConversationService.buildAnswerMemory(memory));
            ragStreamingService.stream(List.of(
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

}
