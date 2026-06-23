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

    /**
     * 创建 SSE 发送器。
     */
    public SseEventSender(SseEmitter emitter, String logName) {
        this.emitter = emitter;
        this.logName = logName;
    }

    /**
     * 发送普通 SSE 事件。
     */
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

    /**
     * 发送错误事件并关闭连接。
     */
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

    /**
     * 正常关闭连接。
     */
    public void complete() {
        if (completed.compareAndSet(false, true)) {
            emitter.complete();
        }
    }

    /**
     * 标记连接已由框架回调关闭。
     */
    public void markCompleted() {
        completed.set(true);
    }
}
