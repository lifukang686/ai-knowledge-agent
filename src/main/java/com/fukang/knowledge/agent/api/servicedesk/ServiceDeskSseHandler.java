package com.fukang.knowledge.agent.api.servicedesk;

import com.fukang.knowledge.agent.api.common.SseEventSender;
import com.fukang.knowledge.agent.application.servicedesk.ServiceDeskStreamHandler;
import com.fukang.knowledge.agent.application.servicedesk.result.ServiceDeskAnswerResult;
import com.fukang.knowledge.agent.domain.agent.model.AgentRunEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * 服务台 SSE 事件适配器。
 * <p>应用层只关心服务台处理过程中的阶段、token、Agent 事件和最终结果；
 * 这里统一转换为前端约定的 SSE 事件名，并把发送失败、错误收尾交给通用发送器处理。</p>
 */
@Slf4j
class ServiceDeskSseHandler implements ServiceDeskStreamHandler {

    /**
     * 统一的 SSE 发送组件，负责连接完成状态、发送异常和错误事件。
     */
    private final SseEventSender sender;

    ServiceDeskSseHandler(SseEmitter emitter) {
        this.sender = new SseEventSender(emitter, "服务台");
    }

    /**
     * 推送服务台处理阶段，例如 Agent 规划、RAG 检索、答案生成等。
     */
    @Override
    public void onStage(String stage, String message) {
        sender.send("stage", Map.of("stage", stage, "message", message));
    }

    /**
     * 推送模型或工具链产生的增量文本。
     */
    @Override
    public void onToken(String token) {
        sender.send("token", Map.of("text", token != null ? token : ""));
    }

    /**
     * 推送 Plan-Execute Runtime 的结构化运行事件，供前端展示 Agent 轨迹。
     */
    @Override
    public void onAgentEvent(AgentRunEvent event) {
        sender.send("agent_event", event);
    }

    /**
     * 推送最终结果并正常关闭 SSE 连接。
     */
    @Override
    public void onDone(ServiceDeskAnswerResult result) {
        sender.send("done", ServiceDeskResponseMapper.toAnswerResp(result));
        sender.complete();
    }

    /**
     * 应用层处理失败时推送 error 事件并关闭连接。
     */
    @Override
    public void onError(String message, Throwable error) {
        log.warn("服务台 SSE 处理失败: {}", message, error);
        completeWithError(message);
    }

    /**
     * Controller 超时、线程池拒绝等入口层异常统一走错误事件收尾。
     */
    void completeWithError(String message) {
        sender.completeWithError(message);
    }

    /**
     * 连接由容器或客户端关闭时只标记状态，避免后续业务线程继续写已关闭连接。
     */
    void markCompleted() {
        sender.markCompleted();
    }
}
