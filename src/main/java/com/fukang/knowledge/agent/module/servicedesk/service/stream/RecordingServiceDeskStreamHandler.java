package com.fukang.knowledge.agent.module.servicedesk.service.stream;

import com.fukang.knowledge.agent.module.agent.model.vo.AgentRunEvent;
import com.fukang.knowledge.agent.module.servicedesk.model.vo.ServiceDeskAnswerResult;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 记录 Agent 事件并转发给真实 SSE 处理器的流式装饰器。
 */
public class RecordingServiceDeskStreamHandler implements ServiceDeskStreamHandler {

    private final ServiceDeskStreamHandler delegate;
    private final List<AgentRunEvent> events = new CopyOnWriteArrayList<>();

    public RecordingServiceDeskStreamHandler(ServiceDeskStreamHandler delegate) {
        this.delegate = delegate;
    }

    /**
     * 返回已记录事件。
     */
    public List<AgentRunEvent> events() {
        return List.copyOf(events);
    }

    @Override
    public void onStage(String stage, String message) {
        if (delegate != null) {
            delegate.onStage(stage, message);
        }
    }

    @Override
    public void onToken(String token) {
        if (delegate != null) {
            delegate.onToken(token);
        }
    }

    @Override
    public void onAgentEvent(AgentRunEvent event) {
        events.add(event);
        if (delegate != null) {
            delegate.onAgentEvent(event);
        }
    }

    @Override
    public void onDone(ServiceDeskAnswerResult result) {
        if (delegate != null) {
            delegate.onDone(result);
        }
    }

    @Override
    public void onError(String message, Throwable error) {
        if (delegate != null) {
            delegate.onError(message, error);
        }
    }
}
