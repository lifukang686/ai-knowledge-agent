package com.fukang.knowledge.agent.module.rag.controller.qa;

import com.fukang.knowledge.agent.module.rag.model.dto.QaConversationCreateReq;
import com.fukang.knowledge.agent.module.rag.model.resp.QaConversationMessageResp;
import com.fukang.knowledge.agent.module.rag.model.resp.QaConversationResp;
import com.fukang.knowledge.agent.module.rag.model.dto.QaReq;
import com.fukang.knowledge.agent.module.rag.model.resp.QaResp;
import com.fukang.knowledge.agent.module.conversation.service.ConversationService;
import com.fukang.knowledge.agent.module.conversation.model.vo.ConversationListItemResult;
import com.fukang.knowledge.agent.module.conversation.model.vo.ConversationMessageResult;
import com.fukang.knowledge.agent.module.rag.service.RagService;
import com.fukang.knowledge.agent.module.rag.service.stream.impl.QaSseStreamHandler;
import com.fukang.knowledge.agent.module.rag.model.vo.QaResult;
import com.fukang.knowledge.agent.common.context.UserContextHolder;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * QA 问答控制器。
 */
@Slf4j
@RestController
@RequestMapping("/api/qa")
public class QaController {

    /**
     * 流式问答 SSE 超时时间。
     */
    private static final long STREAM_TIMEOUT_MS = 120_000L;

    private final RagService ragService;
    private final ConversationService conversationService;
    private final ThreadPoolTaskExecutor aiStreamExecutor;

    /**
     * 创建 QA 控制器。
     */
    public QaController(RagService ragService,
                        ConversationService conversationService,
                        @Qualifier("aiStreamExecutor") ThreadPoolTaskExecutor aiStreamExecutor) {
        this.ragService = ragService;
        this.conversationService = conversationService;
        this.aiStreamExecutor = aiStreamExecutor;
    }

    /**
     * 查询当前用户会话列表。
     */
    @GetMapping("/conversations")
    public Result<List<QaConversationResp>> listConversations(
            @RequestParam(value = "knowledgeBaseId", required = false) Long knowledgeBaseId,
            @RequestParam(value = "limit", required = false) Integer limit) {
        return Result.success(conversationService.listConversations(knowledgeBaseId, limit)
                .stream()
                .map(this::toConversationResp)
                .toList());
    }

    /**
     * 创建空会话。
     */
    @PostMapping("/conversations")
    public Result<QaConversationResp> createConversation(@RequestBody(required = false) QaConversationCreateReq req) {
        Long knowledgeBaseId = req != null ? req.getKnowledgeBaseId() : null;
        return Result.success(toConversationResp(conversationService.createConversation(knowledgeBaseId)));
    }

    /**
     * 查询会话消息。
     */
    @GetMapping("/conversations/{id}/messages")
    public Result<List<QaConversationMessageResp>> listMessages(@PathVariable("id") Long conversationId) {
        return Result.success(conversationService.listMessages(conversationId)
                .stream()
                .map(this::toMessageResp)
                .toList());
    }

    /**
     * 非流式 RAG 问答。
     */
    @PostMapping
    public Result<QaResp> ask(@RequestBody QaReq req) {
        if (req.getQuestion() == null || req.getQuestion().isBlank()) {
            throw new BaseException(ErrorCodeEnum.QUESTION_EMPTY);
        }

        QaResult qaResult = ragService.answer(req.getQuestion(), req.getKnowledgeBaseId(), req.getConversationId());
        return Result.success(new QaResp(
                qaResult.getAnswer(),
                qaResult.getRewrittenQuery(),
                qaResult.getStatus(),
                qaResult.getConversationId()));
    }

    /**
     * 流式 RAG 问答。
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter askStream(@RequestBody QaReq req) {
        if (req.getQuestion() == null || req.getQuestion().isBlank()) {
            throw new BaseException(ErrorCodeEnum.QUESTION_EMPTY);
        }

        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        QaSseStreamHandler handler = new QaSseStreamHandler(emitter);
        emitter.onTimeout(() -> handler.completeWithError("问答生成超时，请稍后重试"));
        emitter.onError(error -> handler.markCompleted());
        emitter.onCompletion(handler::markCompleted);

        Long userId = UserContextHolder.getUserId();
        try {
            aiStreamExecutor.execute(() -> {
                // 异步线程内恢复用户上下文，保证会话和记忆按当前用户写入。
                UserContextHolder.setUserId(userId);
                try {
                    ragService.answerStream(req.getQuestion(), req.getKnowledgeBaseId(), req.getConversationId(), handler);
                } finally {
                    UserContextHolder.clear();
                }
            });
        } catch (RuntimeException e) {
            if (!isTaskRejected(e)) {
                throw e;
            }
            log.warn("QA 流式任务提交被拒绝: activeCount={}, poolSize={}, queueSize={}",
                    aiStreamExecutor.getActiveCount(),
                    aiStreamExecutor.getPoolSize(),
                    aiStreamExecutor.getThreadPoolExecutor().getQueue().size(), e);
            handler.completeWithError("当前问答请求较多，请稍后重试");
        }
        return emitter;
    }

    /**
     * 判断是否为线程池拒绝异常。
     */
    private boolean isTaskRejected(RuntimeException e) {
        return e instanceof org.springframework.core.task.TaskRejectedException
                || e instanceof java.util.concurrent.RejectedExecutionException;
    }

    /**
     * 转换会话响应。
     */
    private QaConversationResp toConversationResp(ConversationListItemResult result) {
        return new QaConversationResp(result.getId(), result.getKnowledgeBaseId(), result.getTitle(), result.getStatus(),
                result.getMessageCount(), result.getLastMessageAt(), result.getCreateTime(), result.getUpdateTime());
    }

    /**
     * 转换会话消息响应。
     */
    private QaConversationMessageResp toMessageResp(ConversationMessageResult result) {
        return new QaConversationMessageResp(result.getId(), result.getConversationId(), result.getRole(), result.getContent(),
                result.getRewrittenQuery(), result.getStatus(), result.getCreateTime(), result.getUpdateTime());
    }
}
