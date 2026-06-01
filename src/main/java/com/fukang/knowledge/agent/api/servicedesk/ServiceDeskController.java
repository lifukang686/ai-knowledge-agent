package com.fukang.knowledge.agent.api.servicedesk;

import com.fukang.knowledge.agent.api.servicedesk.dto.ServiceDeskAnswerResp;
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
import com.fukang.knowledge.agent.application.servicedesk.result.ServiceDeskRunResult;
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

    private static final long STREAM_TIMEOUT_MS = 120_000L;

    private final ServiceDeskAppService serviceDeskAppService;
    private final TicketAppService ticketAppService;
    private final ThreadPoolTaskExecutor qaStreamExecutor;

    public ServiceDeskController(ServiceDeskAppService serviceDeskAppService,
                                 TicketAppService ticketAppService,
                                 @Qualifier("qaStreamExecutor") ThreadPoolTaskExecutor qaStreamExecutor) {
        this.serviceDeskAppService = serviceDeskAppService;
        this.ticketAppService = ticketAppService;
        this.qaStreamExecutor = qaStreamExecutor;
    }

    @PostMapping("/ask")
    public Result<ServiceDeskAnswerResp> ask(@RequestBody ServiceDeskAskReq req) {
        validateQuestion(req);
        ServiceDeskAnswerResult result = serviceDeskAppService.ask(toCommand(req));
        return Result.success(toAnswerResp(result));
    }

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

    @GetMapping("/tickets/{id}")
    public Result<ServiceTicketResp> getTicket(@PathVariable("id") Long id) {
        return Result.success(toTicketResp(ticketAppService.getTicket(id, currentUserId())));
    }

    @PostMapping("/tickets/{id}/confirm")
    public Result<ServiceTicketResp> confirmTicket(@PathVariable("id") Long id) {
        return Result.success(toTicketResp(serviceDeskAppService.confirmTicket(id)));
    }

    @GetMapping("/runs/{runId}")
    public Result<ServiceDeskRunResult> getRun(@PathVariable("runId") Long runId) {
        return Result.success(serviceDeskAppService.getRun(runId));
    }

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

    private void validateQuestion(ServiceDeskAskReq req) {
        if (req == null || req.question() == null || req.question().isBlank()) {
            throw new BaseException(ErrorCodeEnum.BAD_REQUEST.getCode(), "问题不能为空");
        }
    }

    private Long currentUserId() {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new BaseException(ErrorCodeEnum.UNAUTHORIZED);
        }
        return userId;
    }

    private ServiceDeskAskCommand toCommand(ServiceDeskAskReq req) {
        return new ServiceDeskAskCommand(req.question(), req.serviceType(), req.knowledgeBaseId(), req.conversationId());
    }

    private ServiceDeskAnswerResp toAnswerResp(ServiceDeskAnswerResult result) {
        return new ServiceDeskAnswerResp(result.answer(), result.intent(), result.serviceType(), result.status(),
                result.runId(), result.ticketId(), result.ticketNo(), result.conversationId(),
                result.approvalRequired(), result.pendingTicket() != null ? toTicketResp(result.pendingTicket()) : null,
                result.events(), result.feedbackSubmitted());
    }

    private ServiceTicketResp toTicketResp(ServiceTicketResult ticket) {
        return new ServiceTicketResp(ticket.id(), ticket.ticketNo(), ticket.serviceType(), ticket.category(),
                ticket.priority(), ticket.status(), ticket.title(), ticket.description(), ticket.agentSummary(),
                ticket.creatorId(), ticket.assigneeId(), ticket.sourceRunId(), ticket.sourceConversationId(),
                ticket.events().stream().map(this::toTicketEventResp).toList(), ticket.eventCount(),
                ticket.createTime(), ticket.updateTime());
    }

    private ServiceTicketEventResp toTicketEventResp(ServiceTicketEventResult event) {
        return new ServiceTicketEventResp(event.id(), event.ticketId(), event.eventType(), event.fromStatus(),
                event.toStatus(), event.operatorId(), event.message(), event.payload(), event.createTime());
    }

    private ServiceDeskFeedbackResp toFeedbackResp(ServiceDeskFeedbackResult feedback) {
        return new ServiceDeskFeedbackResp(feedback.id(), feedback.runId(), feedback.ticketId(), feedback.resolved(),
                feedback.comment(), feedback.userId(), feedback.createTime());
    }

    /**
     * 服务台 SSE 事件发送器。
     */
    private static class SseServiceDeskHandler implements ServiceDeskStreamHandler {

        private final SseEmitter emitter;
        private final AtomicBoolean completed = new AtomicBoolean(false);

        private SseServiceDeskHandler(SseEmitter emitter) {
            this.emitter = emitter;
        }

        @Override
        public void onStage(String stage, String message) {
            send("stage", Map.of("stage", stage, "message", message));
        }

        @Override
        public void onToken(String token) {
            send("token", Map.of("text", token != null ? token : ""));
        }

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

        @Override
        public void onError(String message, Throwable error) {
            log.warn("服务台 SSE 处理失败: {}", message, error);
            completeWithError(message);
        }

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

        private void complete() {
            if (completed.compareAndSet(false, true)) {
                emitter.complete();
            }
        }

        private void markCompleted() {
            completed.set(true);
        }

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
