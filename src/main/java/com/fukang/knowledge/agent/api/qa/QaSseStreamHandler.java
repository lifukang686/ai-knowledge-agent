package com.fukang.knowledge.agent.api.qa;

import com.fukang.knowledge.agent.api.common.SseEventSender;
import com.fukang.knowledge.agent.application.rag.result.QaResult;
import com.fukang.knowledge.agent.application.rag.stream.QaStreamHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * QA SSE 事件适配器。
 */
@Slf4j
class QaSseStreamHandler implements QaStreamHandler {

    private final SseEventSender sender;

    QaSseStreamHandler(SseEmitter emitter) {
        this.sender = new SseEventSender(emitter, "QA");
    }

    @Override
    public void onStage(String stage, String message) {
        sender.send("stage", Map.of("stage", stage, "message", message));
    }

    @Override
    public void onToken(String token) {
        sender.send("token", Map.of("text", token != null ? token : ""));
    }

    @Override
    public void onDone(QaResult result) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("answer", result.answer() != null ? result.answer() : "");
        payload.put("rewrittenQuery", result.rewrittenQuery() != null ? result.rewrittenQuery() : "");
        payload.put("status", result.status() != null ? result.status() : "success");
        payload.put("conversationId", result.conversationId());
        sender.send("done", payload);
        sender.complete();
    }

    @Override
    public void onError(String message, Throwable error) {
        log.warn("QA SSE 流式问答失败: {}", message, error);
        completeWithError(message);
    }

    void completeWithError(String message) {
        sender.completeWithError(message);
    }

    void markCompleted() {
        sender.markCompleted();
    }
}
