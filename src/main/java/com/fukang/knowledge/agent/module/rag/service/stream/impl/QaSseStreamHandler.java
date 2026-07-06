package com.fukang.knowledge.agent.module.rag.service.stream.impl;

import com.fukang.knowledge.agent.common.api.SseEventSender;
import com.fukang.knowledge.agent.module.rag.model.vo.QaResult;
import com.fukang.knowledge.agent.module.rag.service.stream.QaStreamHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * QA SSE 事件适配器。
 */
@Slf4j
public class QaSseStreamHandler implements QaStreamHandler {

    private final SseEventSender sender;

    /**
     * 创建 QA SSE 处理器。
     */
    public QaSseStreamHandler(SseEmitter emitter) {
        this.sender = new SseEventSender(emitter, "QA");
    }

    /**
     * 发送阶段事件。
     */
    @Override
    public void onStage(String stage, String message) {
        sender.send("stage", Map.of("stage", stage, "message", message));
    }

    /**
     * 发送 token 事件。
     */
    @Override
    public void onToken(String token) {
        sender.send("token", Map.of("text", token != null ? token : ""));
    }

    /**
     * 发送完成事件。
     */
    @Override
    public void onDone(QaResult result) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("answer", result.getAnswer() != null ? result.getAnswer() : "");
        payload.put("rewrittenQuery", result.getRewrittenQuery() != null ? result.getRewrittenQuery() : "");
        payload.put("status", result.getStatus() != null ? result.getStatus() : "success");
        payload.put("conversationId", result.getConversationId());
        sender.send("done", payload);
        sender.complete();
    }

    /**
     * 发送错误事件。
     */
    @Override
    public void onError(String message, Throwable error) {
        log.warn("QA SSE 流式问答失败: {}", message, error);
        completeWithError(message);
    }

    /**
     * 主动以错误关闭连接。
     */
    public void completeWithError(String message) {
        sender.completeWithError(message);
    }

    /**
     * 标记连接完成。
     */
    public void markCompleted() {
        sender.markCompleted();
    }
}
