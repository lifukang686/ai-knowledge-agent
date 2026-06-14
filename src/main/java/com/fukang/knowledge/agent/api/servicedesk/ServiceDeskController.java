package com.fukang.knowledge.agent.api.servicedesk;

import com.fukang.knowledge.agent.api.servicedesk.dto.ServiceDeskAskReq;
import com.fukang.knowledge.agent.api.servicedesk.dto.ServiceDeskFeedbackReq;
import com.fukang.knowledge.agent.api.servicedesk.dto.ServiceDeskFeedbackResp;
import com.fukang.knowledge.agent.api.servicedesk.dto.ServiceTicketResp;
import com.fukang.knowledge.agent.application.servicedesk.ServiceDeskAppService;
import com.fukang.knowledge.agent.application.servicedesk.TicketAppService;
import com.fukang.knowledge.agent.application.servicedesk.command.ServiceDeskAskCommand;
import com.fukang.knowledge.agent.application.servicedesk.command.SubmitFeedbackCommand;
import com.fukang.knowledge.agent.application.servicedesk.result.ServiceDeskFeedbackResult;
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
    private final ThreadPoolTaskExecutor aiStreamExecutor;

    public ServiceDeskController(ServiceDeskAppService serviceDeskAppService,
                                 TicketAppService ticketAppService,
                                 @Qualifier("aiStreamExecutor") ThreadPoolTaskExecutor aiStreamExecutor) {
        this.serviceDeskAppService = serviceDeskAppService;
        this.ticketAppService = ticketAppService;
        this.aiStreamExecutor = aiStreamExecutor;
    }

    /**
     * 流式提交服务台问题。
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter askStream(@RequestBody ServiceDeskAskReq req) {
        validateQuestion(req);
        Long userId = currentUserId();

        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        ServiceDeskSseHandler handler = new ServiceDeskSseHandler(emitter);
        emitter.onTimeout(() -> handler.completeWithError("服务台处理超时，请稍后重试"));
        emitter.onError(error -> handler.markCompleted());
        emitter.onCompletion(handler::markCompleted);

        try {
            aiStreamExecutor.execute(() -> serviceDeskAppService.askStreamAsUser(toCommand(req), userId, handler));
        } catch (RuntimeException e) {
            if (!isTaskRejected(e)) {
                throw e;
            }
            log.warn("服务台流式任务提交被拒绝: activeCount={}, poolSize={}, queueSize={}",
                    aiStreamExecutor.getActiveCount(),
                    aiStreamExecutor.getPoolSize(),
                    aiStreamExecutor.getThreadPoolExecutor().getQueue().size(), e);
            handler.completeWithError("服务台当前请求较多，请稍后重试");
        }
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
                tickets.getItems().stream().map(ServiceDeskResponseMapper::toTicketResp).toList(),
                tickets.getTotal(),
                tickets.getPage(),
                tickets.getPageSize()));
    }

    /**
     * 确认工单处理完成。
     */
    @PostMapping("/tickets/{id}/confirm")
    public Result<ServiceTicketResp> confirmTicket(@PathVariable("id") Long id) {
        return Result.success(ServiceDeskResponseMapper.toTicketResp(serviceDeskAppService.confirmTicket(id)));
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
        return Result.success(ServiceDeskResponseMapper.toFeedbackResp(feedback));
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

    private boolean isTaskRejected(RuntimeException e) {
        return e instanceof org.springframework.core.task.TaskRejectedException
                || e instanceof java.util.concurrent.RejectedExecutionException;
    }
}
