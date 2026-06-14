package com.fukang.knowledge.agent.application.servicedesk;

import com.fukang.knowledge.agent.application.servicedesk.result.ServiceDeskAnswerResult;
import com.fukang.knowledge.agent.domain.agent.model.AgentRunEvent;

/**
 * 服务台流式事件回调。
 */
public interface ServiceDeskStreamHandler {

    void onStage(String stage, String message);

    void onToken(String token);

    default void onAgentEvent(AgentRunEvent event) {
    }

    void onDone(ServiceDeskAnswerResult result);

    void onError(String message, Throwable error);
}
