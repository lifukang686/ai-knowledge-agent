package com.fukang.knowledge.agent.application.servicedesk;

import com.fukang.knowledge.agent.application.servicedesk.result.ServiceDeskAnswerResult;

/**
 * 服务台流式事件回调。
 */
public interface ServiceDeskStreamHandler {

    void onStage(String stage, String message);

    void onToken(String token);

    void onDone(ServiceDeskAnswerResult result);

    void onError(String message, Throwable error);
}
