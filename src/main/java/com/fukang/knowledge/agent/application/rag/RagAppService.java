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
 * <p>串联意图识别、查询改写、知识库检索、重排序、回答生成和会话记忆保存。</p>
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

    private static final String STATUS_SUCCESS = "success";
    private static final String STATUS_NO_RESULTS = "no_results";
    private static final String NO_RESULT_ANSWER = "抱歉，未找到与您问题相关的文档内容。";

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

        if (shouldBypassRetrieval(question, intent)) {
            log.info("Bypass RAG retrieval: intent={}, question={}", intent, question);
            return ragDirectAnswerService.answerByIntent(question, memory, intent);
        }

        String rewrittenQuery = queryRewritePort.rewriteWithHistory(
                question, memory.summary(), memory.rewriteHistory());
        List<SearchResult> retrieved = ragRetrievalService.retrieveWithFallback(rewrittenQuery, question, knowledgeBaseId);

        if (retrieved.isEmpty() && isChitchat(rewrittenQuery)) {
            log.info("Fallback to direct chat because retrieval is empty and rewritten query is chitchat");
            return ragDirectAnswerService.answerDirectChat(rewrittenQuery, question, rewrittenQuery, memory);
        }

        List<SearchResult> reranked = reranker.rerank(retrieved, question);
        String status = reranked.isEmpty() ? STATUS_NO_RESULTS : STATUS_SUCCESS;
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
            // 准备记忆
            RagStreamContext context = prepareStreamMemory(question, knowledgeBaseId, conversationId);
            // 意图识别
            recognizeStreamIntent(context);
            if (tryStreamDirectAnswer(context, handler)) {
                return;
            }

            // 查询重写
            rewriteStreamQuery(context, handler);
            // 检索知识库
            retrieveStreamKnowledge(context, handler);
            if (tryFallbackToStreamDirectChat(context, handler)) {
                return;
            }

            // 重排序检索结果
            rerankStreamResults(context, handler);
            if (tryStreamNoResultAnswer(context, handler)) {
                return;
            }

            // 回答
            streamAnswer(context, handler);
        } catch (Exception e) {
            log.error("RAG流式回答失败", e);
            handler.onError("生成失败，请稍后重试", e);
        }
    }

    /**
     * 准备会话记忆。
     */
    private RagStreamContext prepareStreamMemory(String question, Long knowledgeBaseId, Long conversationId) {
        validateKnowledgeBase(knowledgeBaseId);
        ConversationMemoryContext memory = ragConversationService.prepareContext(conversationId, knowledgeBaseId, question);
        log.info("流式问答准备记忆完成: conversationId={}, knowledgeBaseId={}",
                memory.conversationId(), knowledgeBaseId);
        return new RagStreamContext(question, knowledgeBaseId, memory);
    }

    /**
     * 识别问题意图。
     */
    private void recognizeStreamIntent(RagStreamContext context) {
        context.intent = qaIntentClassifier.classify(context.question);
        log.info("流式问答意图识别完成: intent={}", context.intent);
    }

    /**
     * 尝试直接回答。
     */
    private boolean tryStreamDirectAnswer(RagStreamContext context, QaStreamHandler handler) {
        if (!shouldBypassRetrieval(context.question, context.intent)) {
            return false;
        }
        log.info("流式问答走直接回答: intent={}", context.intent);
        handler.onStage("generate_start", "检测到非知识库问题，正在直接回答");
        ragDirectAnswerService.streamByIntent(context.question, context.memory, context.intent, handler);
        return true;
    }

    /**
     * 改写检索查询。
     */
    private void rewriteStreamQuery(RagStreamContext context, QaStreamHandler handler) {
        handler.onStage("rewrite_start", "正在结合会话历史改写查询");
        context.rewrittenQuery = queryRewritePort.rewriteWithHistory(
                context.question, context.memory.summary(), context.memory.rewriteHistory());
        log.info("流式问答查询改写完成");
        handler.onStage("rewrite_done", "查询改写完成");
    }

    /**
     * 检索知识库。
     */
    private void retrieveStreamKnowledge(RagStreamContext context, QaStreamHandler handler) {
        handler.onStage("retrieve_start", "正在检索知识库");
        context.retrieved = ragRetrievalService.retrieveWithFallback(
                context.rewrittenQuery, context.question, context.knowledgeBaseId);
        log.info("流式问答检索完成: candidateCount={}", context.retrieved.size());
        handler.onStage("retrieve_done", "检索完成，找到 " + context.retrieved.size() + " 个候选片段");
    }

    /**
     * 检索为空时降级直接回答。
     */
    private boolean tryFallbackToStreamDirectChat(RagStreamContext context, QaStreamHandler handler) {
        if (!context.retrieved.isEmpty() || !isChitchat(context.rewrittenQuery)) {
            return false;
        }
        log.info("流式问答检索为空，降级直接回答");
        handler.onStage("generate_start", "检索无结果，降级为直接回答");
        ragDirectAnswerService.streamDirectChat(
                context.rewrittenQuery, context.rewrittenQuery, context.rewrittenQuery, context.memory, handler);
        return true;
    }

    /**
     * 重排序检索结果。
     */
    private void rerankStreamResults(RagStreamContext context, QaStreamHandler handler) {
        handler.onStage("rerank_start", "正在对检索结果重排序");
        context.reranked = reranker.rerank(context.retrieved, context.question);
        log.info("流式问答重排序完成: keptCount={}", context.reranked.size());
        handler.onStage("rerank_done", "重排序完成，保留 " + context.reranked.size() + " 个候选片段");
    }

    /**
     * 无召回结果时返回兜底回答。
     */
    private boolean tryStreamNoResultAnswer(RagStreamContext context, QaStreamHandler handler) {
        if (!context.reranked.isEmpty()) {
            return false;
        }
        log.info("流式问答无可用片段，返回无结果回答");
        ragConversationService.saveTurn(
                context.memory.conversationId(), context.question, context.rewrittenQuery, NO_RESULT_ANSWER, STATUS_NO_RESULTS);
        handler.onToken(NO_RESULT_ANSWER);
        handler.onDone(new QaResult(
                NO_RESULT_ANSWER, context.rewrittenQuery, STATUS_NO_RESULTS, context.memory.conversationId()));
        return true;
    }

    /**
     * 流式生成回答。
     */
    private void streamAnswer(RagStreamContext context, QaStreamHandler handler) {
        handler.onStage("generate_start", "正在生成回答");
        log.info("流式问答开始生成回答: chunkCount={}", context.reranked.size());
        String systemPrompt = promptTemplateManager.renderText("rag/answer-system.v1", null);
        String userPrompt = llmAnswerGenerator.buildRagUserPrompt(context.reranked, context.rewrittenQuery,
                ragConversationService.buildAnswerMemory(context.memory));
        ragStreamingService.stream(List.of(
                ChatCompletionPort.Message.system(systemPrompt),
                ChatCompletionPort.Message.user(userPrompt)
        ), context.question, context.rewrittenQuery, STATUS_SUCCESS, context.memory.conversationId(), handler);
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

    private static class RagStreamContext {

        private final String question;
        private final Long knowledgeBaseId;
        private final ConversationMemoryContext memory;

        private QaIntent intent;
        private String rewrittenQuery;
        private List<SearchResult> retrieved = List.of();
        private List<SearchResult> reranked = List.of();

        private RagStreamContext(String question, Long knowledgeBaseId, ConversationMemoryContext memory) {
            this.question = question;
            this.knowledgeBaseId = knowledgeBaseId;
            this.memory = memory;
        }
    }
}
