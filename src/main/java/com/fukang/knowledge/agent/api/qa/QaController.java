package com.fukang.knowledge.agent.api.qa;

import com.fukang.knowledge.agent.api.qa.dto.QaConversationCreateReq;
import com.fukang.knowledge.agent.api.qa.dto.QaConversationMessageResp;
import com.fukang.knowledge.agent.api.qa.dto.QaConversationResp;
import com.fukang.knowledge.agent.api.qa.dto.QaReq;
import com.fukang.knowledge.agent.api.qa.dto.QaResp;
import com.fukang.knowledge.agent.application.conversation.ConversationAppService;
import com.fukang.knowledge.agent.application.conversation.result.ConversationListItemResult;
import com.fukang.knowledge.agent.application.conversation.result.ConversationMessageResult;
import com.fukang.knowledge.agent.application.rag.RagAppService;
import com.fukang.knowledge.agent.application.rag.result.QaResult;
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

    private static final long STREAM_TIMEOUT_MS = 120_000L;

    private final RagAppService ragAppService;
    private final ConversationAppService conversationAppService;
    private final ThreadPoolTaskExecutor aiStreamExecutor;

    public QaController(RagAppService ragAppService,
                        ConversationAppService conversationAppService,
                        @Qualifier("aiStreamExecutor") ThreadPoolTaskExecutor aiStreamExecutor) {
        this.ragAppService = ragAppService;
        this.conversationAppService = conversationAppService;
        this.aiStreamExecutor = aiStreamExecutor;
    }

    @GetMapping("/conversations")
    public Result<List<QaConversationResp>> listConversations(
            @RequestParam(value = "knowledgeBaseId", required = false) Long knowledgeBaseId,
            @RequestParam(value = "limit", required = false) Integer limit) {
        return Result.success(conversationAppService.listConversations(knowledgeBaseId, limit)
                .stream()
                .map(this::toConversationResp)
                .toList());
    }

    @PostMapping("/conversations")
    public Result<QaConversationResp> createConversation(@RequestBody(required = false) QaConversationCreateReq req) {
        Long knowledgeBaseId = req != null ? req.knowledgeBaseId() : null;
        return Result.success(toConversationResp(conversationAppService.createConversation(knowledgeBaseId)));
    }

    @GetMapping("/conversations/{id}/messages")
    public Result<List<QaConversationMessageResp>> listMessages(@PathVariable("id") Long conversationId) {
        return Result.success(conversationAppService.listMessages(conversationId)
                .stream()
                .map(this::toMessageResp)
                .toList());
    }

    @PostMapping
    public Result<QaResp> ask(@RequestBody QaReq req) {
        if (req.question() == null || req.question().isBlank()) {
            throw new BaseException(ErrorCodeEnum.QUESTION_EMPTY);
        }

        QaResult qaResult = ragAppService.answer(req.question(), req.knowledgeBaseId(), req.conversationId());
        return Result.success(new QaResp(
                qaResult.answer(),
                qaResult.rewrittenQuery(),
                qaResult.status(),
                qaResult.conversationId()));
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter askStream(@RequestBody QaReq req) {
        if (req.question() == null || req.question().isBlank()) {
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
                UserContextHolder.setUserId(userId);
                try {
                    ragAppService.answerStream(req.question(), req.knowledgeBaseId(), req.conversationId(), handler);
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

    private boolean isTaskRejected(RuntimeException e) {
        return e instanceof org.springframework.core.task.TaskRejectedException
                || e instanceof java.util.concurrent.RejectedExecutionException;
    }

    private QaConversationResp toConversationResp(ConversationListItemResult result) {
        return new QaConversationResp(result.id(), result.knowledgeBaseId(), result.title(), result.status(),
                result.messageCount(), result.lastMessageAt(), result.createTime(), result.updateTime());
    }

    private QaConversationMessageResp toMessageResp(ConversationMessageResult result) {
        return new QaConversationMessageResp(result.id(), result.conversationId(), result.role(), result.content(),
                result.rewrittenQuery(), result.status(), result.createTime(), result.updateTime());
    }
}
