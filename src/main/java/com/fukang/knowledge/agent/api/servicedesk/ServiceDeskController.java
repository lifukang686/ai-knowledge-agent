package com.fukang.knowledge.agent.api.servicedesk;

import com.fukang.knowledge.agent.api.servicedesk.dto.ServiceDeskAskReq;
import com.fukang.knowledge.agent.api.servicedesk.dto.ServiceDeskFeedbackReq;
import com.fukang.knowledge.agent.api.servicedesk.dto.ServiceDeskFeedbackResp;
import com.fukang.knowledge.agent.api.servicedesk.dto.ServiceTicketEventResp;
import com.fukang.knowledge.agent.api.servicedesk.dto.ServiceTicketResp;
import com.fukang.knowledge.agent.application.servicedesk.ServiceDeskAppService;
import com.fukang.knowledge.agent.application.servicedesk.ServiceDeskStreamHandler;
import com.fukang.knowledge.agent.application.servicedesk.TicketAppService;
import com.fukang.knowledge.agent.application.servicedesk.command.ServiceDeskAskCommand;
import com.fukang.knowledge.agent.application.servicedesk.command.SubmitFeedbackCommand;
import com.fukang.knowledge.agent.application.servicedesk.result.ServiceDeskAnswerResult;
import com.fukang.knowledge.agent.application.servicedesk.result.ServiceDeskFeedbackResult;
import com.fukang.knowledge.agent.application.servicedesk.result.ServiceTicketEventResult;
import com.fukang.knowledge.agent.application.servicedesk.result.ServiceTicketResult;
import com.fukang.knowledge.agent.common.context.UserContextHolder;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.common.result.PageResponse;
import com.fukang.knowledge.agent.common.result.Result;
import com.fukang.knowledge.agent.domain.servicedesk.ServiceType;
import com.fukang.knowledge.agent.domain.servicedesk.TicketStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 企业 IT/HR 服务台 Agent 控制器。
 */
@Slf4j
@RestController
@RequestMapping("/api/service-desk")
public class ServiceDeskController {

    /**
     * SSE 流式响应超时时间。
     */
    private static final long STREAM_TIMEOUT_MS = 120_000L;

    /**
     * 服务台问答应用服务。
     */
    private final ServiceDeskAppService serviceDeskAppService;

    /**
     * 工单应用服务。
     */
    private final TicketAppService ticketAppService;

    /**
     * 问答流式执行线程池。
     */
    private final ThreadPoolTaskExecutor qaStreamExecutor;

    /**
     * 创建服务台控制器。
     */
    public ServiceDeskController(ServiceDeskAppService serviceDeskAppService,
                                 TicketAppService ticketAppService,
                                 @Qualifier("qaStreamExecutor") ThreadPoolTaskExecutor qaStreamExecutor) {
        this.serviceDeskAppService = serviceDeskAppService;
        this.ticketAppService = ticketAppService;
        this.qaStreamExecutor = qaStreamExecutor;
    }

    /**
     * 流式提交服务台问题。
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter askStream(@RequestBody ServiceDeskAskReq req) {
        validateQuestion(req);
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new BaseException(ErrorCodeEnum.UNAUTHORIZED);
        }

        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        SseServiceDeskHandler handler = new SseServiceDeskHandler(emitter);
        emitter.onTimeout(() -> handler.completeWithError("服务台处理超时，请稍后重试"));
        emitter.onError(error -> handler.markCompleted());
        emitter.onCompletion(handler::markCompleted);

        qaStreamExecutor.execute(() -> serviceDeskAppService.askStreamAsUser(toCommand(req), userId, handler));
        return emitter;
    }

    /**
     * 分页查询当前用户工单。
     */
    @GetMapping("/tickets")
    public Result<PageResponse<ServiceTicketResp>> listTickets(
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "pageSize", defaultValue = "20") long pageSize,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "serviceType", required = false) String serviceType) {
        PageResponse<ServiceTicketResult> tickets = ticketAppService.listTickets(
                currentUserId(), page, pageSize, TicketStatus.from(status), ServiceType.from(serviceType));
        return Result.success(new PageResponse<>(
                tickets.getItems().stream().map(this::toTicketResp).toList(),
                tickets.getTotal(),
                tickets.getPage(),
                tickets.getPageSize()));
    }

    /**
     * 确认工单处理完成。
     */
    @PostMapping("/tickets/{id}/confirm")
    public Result<ServiceTicketResp> confirmTicket(@PathVariable("id") Long id) {
        return Result.success(toTicketResp(serviceDeskAppService.confirmTicket(id)));
    }

    /**
     * 提交服务台运行反馈。
     */
    @PostMapping("/runs/{runId}/feedback")
    public Result<ServiceDeskFeedbackResp> submitFeedback(@PathVariable("runId") Long runId,
                                                          @RequestBody ServiceDeskFeedbackReq req) {
        if (req == null || req.resolved() == null) {
            throw new BaseException(ErrorCodeEnum.BAD_REQUEST.getCode(), "反馈结果不能为空");
        }
        ServiceDeskFeedbackResult feedback = serviceDeskAppService.submitFeedback(
                new SubmitFeedbackCommand(runId, currentUserId(), req.resolved(), req.comment()));
        return Result.success(toFeedbackResp(feedback));
    }

    /**
     * 校验问题参数。
     */
    private void validateQuestion(ServiceDeskAskReq req) {
        if (req == null || req.question() == null || req.question().isBlank()) {
            throw new BaseException(ErrorCodeEnum.BAD_REQUEST.getCode(), "问题不能为空");
        }
    }

    /**
     * 获取当前登录用户 ID。
     */
    private Long currentUserId() {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new BaseException(ErrorCodeEnum.UNAUTHORIZED);
        }
        return userId;
    }

    /**
     * 转换问答命令。
     */
    private ServiceDeskAskCommand toCommand(ServiceDeskAskReq req) {
        return new ServiceDeskAskCommand(req.question(), req.serviceType(), req.knowledgeBaseId(), req.conversationId());
    }

    /**
     * 转换工单响应。
     */
    private ServiceTicketResp toTicketResp(ServiceTicketResult ticket) {
        return new ServiceTicketResp(ticket.id(), ticket.ticketNo(), ticket.serviceType(), ticket.category(),
                ticket.priority(), ticket.status(), ticket.title(), ticket.description(), ticket.agentSummary(),
                ticket.creatorId(), ticket.assigneeId(), ticket.sourceRunId(), ticket.sourceConversationId(),
                ticket.events().stream().map(this::toTicketEventResp).toList(), ticket.eventCount(),
                ticket.createTime(), ticket.updateTime());
    }

    /**
     * 转换工单事件响应。
     */
    private ServiceTicketEventResp toTicketEventResp(ServiceTicketEventResult event) {
        return new ServiceTicketEventResp(event.id(), event.ticketId(), event.eventType(), event.fromStatus(),
                event.toStatus(), event.operatorId(), event.message(), event.payload(), event.createTime());
    }

    /**
     * 转换反馈响应。
     */
    private ServiceDeskFeedbackResp toFeedbackResp(ServiceDeskFeedbackResult feedback) {
        return new ServiceDeskFeedbackResp(feedback.id(), feedback.runId(), feedback.ticketId(), feedback.resolved(),
                feedback.comment(), feedback.userId(), feedback.createTime());
    }

    /**
     * 服务台 SSE 事件发送器。
     */
    private static class SseServiceDeskHandler implements ServiceDeskStreamHandler {

        /**
         * SSE 响应发送器。
         */
        private final SseEmitter emitter;

        /**
         * 完成状态标记。
         */
        private final AtomicBoolean completed = new AtomicBoolean(false);

        /**
         * 创建 SSE 事件处理器。
         */
        private SseServiceDeskHandler(SseEmitter emitter) {
            this.emitter = emitter;
        }

        /**
         * 发送阶段事件。
         */
        @Override
        public void onStage(String stage, String message) {
            send("stage", Map.of("stage", stage, "message", message));
        }

        /**
         * 发送回答 token。
         */
        @Override
        public void onToken(String token) {
            send("token", Map.of("text", token != null ? token : ""));
        }

        /**
         * 发送完成事件。
         */
        @Override
        public void onDone(ServiceDeskAnswerResult result) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("answer", result.answer() != null ? result.answer() : "");
            payload.put("intent", result.intent() != null ? result.intent() : "");
            payload.put("serviceType", result.serviceType() != null ? result.serviceType() : "");
            payload.put("status", result.status() != null ? result.status() : "success");
            payload.put("runId", result.runId());
            payload.put("ticketId", result.ticketId());
            payload.put("ticketNo", result.ticketNo());
            payload.put("conversationId", result.conversationId());
            payload.put("approvalRequired", result.approvalRequired());
            payload.put("pendingTicket", result.pendingTicket() != null ? toTicketMap(result.pendingTicket()) : null);
            payload.put("events", result.events());
            payload.put("feedbackSubmitted", result.feedbackSubmitted());
            send("done", payload);
            complete();
        }

        /**
         * 发送错误事件。
         */
        @Override
        public void onError(String message, Throwable error) {
            log.warn("服务台 SSE 处理失败: {}", message, error);
            completeWithError(message);
        }

        /**
         * 发送 SSE 事件。
         */
        private void send(String eventName, Object data) {
            if (completed.get()) {
                return;
            }
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException | IllegalStateException e) {
                completed.set(true);
                log.warn("发送服务台 SSE 事件失败: event={}", eventName, e);
            }
        }

        /**
         * 发送错误并结束流。
         */
        private void completeWithError(String message) {
            if (!completed.compareAndSet(false, true)) {
                return;
            }
            try {
                emitter.send(SseEmitter.event().name("error").data(Map.of("message", message)));
            } catch (IOException | IllegalStateException e) {
                log.warn("发送服务台 SSE 错误事件失败", e);
            } finally {
                emitter.complete();
            }
        }

        /**
         * 正常结束流。
         */
        private void complete() {
            if (completed.compareAndSet(false, true)) {
                emitter.complete();
            }
        }

        /**
         * 标记流已结束。
         */
        private void markCompleted() {
            completed.set(true);
        }

        /**
         * 转换工单数据。
         */
        private Map<String, Object> toTicketMap(ServiceTicketResult ticket) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", ticket.id());
            data.put("ticketNo", ticket.ticketNo());
            data.put("serviceType", ticket.serviceType());
            data.put("category", ticket.category());
            data.put("priority", ticket.priority());
            data.put("status", ticket.status());
            data.put("title", ticket.title());
            data.put("description", ticket.description());
            data.put("agentSummary", ticket.agentSummary());
            data.put("creatorId", ticket.creatorId());
            data.put("assigneeId", ticket.assigneeId());
            data.put("sourceRunId", ticket.sourceRunId());
            data.put("sourceConversationId", ticket.sourceConversationId());
            data.put("events", ticket.events());
            data.put("eventCount", ticket.eventCount());
            data.put("createTime", ticket.createTime());
            data.put("updateTime", ticket.updateTime());
            return data;
        }
    }
}
