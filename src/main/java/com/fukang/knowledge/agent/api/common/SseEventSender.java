package com.fukang.knowledge.agent.api.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * SSE 事件发送器，统一处理发送失败、错误事件和连接完成状态。
 */
@Slf4j
public class SseEventSender {

    private final SseEmitter emitter;
    private final String logName;
    private final AtomicBoolean completed = new AtomicBoolean(false);

    public SseEventSender(SseEmitter emitter, String logName) {
        this.emitter = emitter;
        this.logName = logName;
    }

    public void send(String eventName, Object data) {
        if (completed.get()) {
            return;
        }
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException | IllegalStateException e) {
            completed.set(true);
            log.warn("发送 {} SSE 事件失败: event={}", logName, eventName, e);
        }
    }

    public void completeWithError(String message) {
        if (!completed.compareAndSet(false, true)) {
            return;
        }
        try {
            emitter.send(SseEmitter.event().name("error").data(Map.of("message", message)));
        } catch (IOException | IllegalStateException e) {
            log.warn("发送 {} SSE 错误事件失败", logName, e);
        } finally {
            emitter.complete();
        }
    }

    public void complete() {
        if (completed.compareAndSet(false, true)) {
            emitter.complete();
        }
    }

    public void markCompleted() {
        completed.set(true);
    }
}
