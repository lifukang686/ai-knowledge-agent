package com.fukang.knowledge.agent.module.servicedesk.service.stream;

import com.fukang.knowledge.agent.module.servicedesk.model.vo.ServiceDeskAnswerResult;
import com.fukang.knowledge.agent.module.agent.model.vo.AgentRunEvent;

/**
 * 服务台流式事件回调。
 */
public interface ServiceDeskStreamHandler {

    /**
     * 推送当前处理阶段。
     */
    void onStage(String stage, String message);

    /**
     * 推送流式文本片段。
     */
    void onToken(String token);

    /**
     * 推送 Agent 运行轨迹事件。
     */
    default void onAgentEvent(AgentRunEvent event) {
    }

    /**
     * 推送最终处理结果。
     */
    void onDone(ServiceDeskAnswerResult result);

    /**
     * 推送处理失败信息。
     */
    void onError(String message, Throwable error);
}
