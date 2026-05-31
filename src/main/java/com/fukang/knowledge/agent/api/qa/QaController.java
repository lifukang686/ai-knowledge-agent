package com.fukang.knowledge.agent.api.qa;

import com.fukang.knowledge.agent.api.qa.dto.QaReq;
import com.fukang.knowledge.agent.api.qa.dto.QaResp;
import com.fukang.knowledge.agent.application.rag.RagAppService;
import com.fukang.knowledge.agent.application.rag.result.QaResult;
import com.fukang.knowledge.agent.application.rag.stream.QaStreamHandler;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * QA 问答控制器
 * <p>提供基于 RAG 的知识库智能问答接口，支持多轮对话上下文关联</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/qa")
public class QaController {

    private static final long STREAM_TIMEOUT_MS = 120_000L;

    private final RagAppService ragAppService;
    private final ThreadPoolTaskExecutor qaStreamExecutor;

    public QaController(RagAppService ragAppService,
                        @Qualifier("qaStreamExecutor") ThreadPoolTaskExecutor qaStreamExecutor) {
        this.ragAppService = ragAppService;
        this.qaStreamExecutor = qaStreamExecutor;
    }

    /**
     * 知识库智能问答
     * <p>接收用户自然语言问题，经过查询改写后在指定知识库中检索相关内容，
     * 结合大语言模型生成回答。支持通过 conversationId 关联多轮对话上下文。
     *
     * @param req 问答请求参数，question 必填
     * @return 问答响应，包含回答文本和改写后的查询
     */
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

    /**
     * 知识库智能问答流式接口。
     * <p>检索、重排等阶段返回 stage 事件，生成阶段通过 token 事件增量返回内容。</p>
     *
     * @param req 问答请求参数，question 必填
     * @return SSE 事件流
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter askStream(@RequestBody QaReq req) {
        if (req.question() == null || req.question().isBlank()) {
            throw new BaseException(ErrorCodeEnum.QUESTION_EMPTY);
        }

        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        SseQaStreamHandler handler = new SseQaStreamHandler(emitter);
        emitter.onTimeout(() -> handler.completeWithError("问答生成超时，请稍后重试"));
        emitter.onError(error -> handler.markCompleted());
        emitter.onCompletion(handler::markCompleted);

        qaStreamExecutor.execute(() ->
                ragAppService.answerStream(req.question(), req.knowledgeBaseId(), req.conversationId(), handler));
        return emitter;
    }

    /**
     * 将应用层流式回调转换为前端可消费的 SSE 事件。
     */
    private static class SseQaStreamHandler implements QaStreamHandler {

        private final SseEmitter emitter;
        private final AtomicBoolean completed = new AtomicBoolean(false);

        private SseQaStreamHandler(SseEmitter emitter) {
            this.emitter = emitter;
        }

        @Override
        public void onStage(String stage, String message) {
            send("stage", Map.of("stage", stage, "message", message));
        }

        @Override
        public void onToken(String token) {
            send("token", Map.of("text", token != null ? token : ""));
        }

        @Override
        public void onDone(QaResult result) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("answer", result.answer() != null ? result.answer() : "");
            payload.put("rewrittenQuery", result.rewrittenQuery() != null ? result.rewrittenQuery() : "");
            payload.put("status", result.status() != null ? result.status() : "success");
            payload.put("conversationId", result.conversationId());
            send("done", payload);
            complete();
        }

        @Override
        public void onError(String message, Throwable error) {
            log.warn("QA SSE 流式问答失败: {}", message, error);
            completeWithError(message);
        }

        private void send(String eventName, Object data) {
            if (completed.get()) {
                return;
            }
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException | IllegalStateException e) {
                completed.set(true);
                log.warn("发送 QA SSE 事件失败: event={}", eventName, e);
            }
        }

        private void completeWithError(String message) {
            if (!completed.compareAndSet(false, true)) {
                return;
            }
            try {
                emitter.send(SseEmitter.event().name("error").data(Map.of("message", message)));
            } catch (IOException | IllegalStateException e) {
                log.warn("发送 QA SSE 错误事件失败", e);
            } finally {
                emitter.complete();
            }
        }

        private void complete() {
            if (completed.compareAndSet(false, true)) {
                emitter.complete();
            }
        }

        private void markCompleted() {
            completed.set(true);
        }
    }
}
