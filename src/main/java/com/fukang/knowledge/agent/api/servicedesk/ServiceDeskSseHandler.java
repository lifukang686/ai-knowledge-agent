package com.fukang.knowledge.agent.api.servicedesk;

import com.fukang.knowledge.agent.api.common.SseEventSender;
import com.fukang.knowledge.agent.application.servicedesk.ServiceDeskStreamHandler;
import com.fukang.knowledge.agent.application.servicedesk.result.ServiceDeskAnswerResult;
import com.fukang.knowledge.agent.domain.agent.model.AgentRunEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * 服务台 SSE 事件适配器，将应用层回调转换为前端可消费的事件。
 */
@Slf4j
class ServiceDeskSseHandler implements ServiceDeskStreamHandler {

    private final SseEventSender sender;

    ServiceDeskSseHandler(SseEmitter emitter) {
        this.sender = new SseEventSender(emitter, "服务台");
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
    public void onAgentEvent(AgentRunEvent event) {
        sender.send("agent_event", event);
    }

    @Override
    public void onDone(ServiceDeskAnswerResult result) {
        sender.send("done", ServiceDeskResponseMapper.toAnswerResp(result));
        sender.complete();
    }

    @Override
    public void onError(String message, Throwable error) {
        log.warn("服务台 SSE 处理失败: {}", message, error);
        completeWithError(message);
    }

    void completeWithError(String message) {
        sender.completeWithError(message);
    }

    void markCompleted() {
        sender.markCompleted();
    }
}
