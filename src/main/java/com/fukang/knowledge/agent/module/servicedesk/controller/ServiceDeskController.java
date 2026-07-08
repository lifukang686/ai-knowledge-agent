package com.fukang.knowledge.agent.module.servicedesk.controller;

import com.fukang.knowledge.agent.module.servicedesk.model.dto.ServiceDeskAskReq;
import com.fukang.knowledge.agent.module.servicedesk.model.dto.ServiceDeskFeedbackReq;
import com.fukang.knowledge.agent.module.servicedesk.model.resp.ServiceDeskFeedbackResp;
import com.fukang.knowledge.agent.module.servicedesk.model.resp.ServiceDeskResponseMapper;
import com.fukang.knowledge.agent.module.servicedesk.model.resp.ServiceTicketResp;
import com.fukang.knowledge.agent.module.servicedesk.service.stream.ServiceDeskSseHandler;
import com.fukang.knowledge.agent.module.servicedesk.service.ServiceDeskService;
import com.fukang.knowledge.agent.module.servicedesk.service.TicketService;
import com.fukang.knowledge.agent.module.servicedesk.model.dto.ServiceDeskAskCommand;
import com.fukang.knowledge.agent.module.servicedesk.model.vo.ServiceDeskFeedbackResult;
import com.fukang.knowledge.agent.module.servicedesk.model.vo.ServiceTicketResult;
import com.fukang.knowledge.agent.common.context.UserContextHolder;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.common.result.PageResponse;
import com.fukang.knowledge.agent.common.result.Result;
import com.fukang.knowledge.agent.common.enums.ServiceTypeEnum;
import com.fukang.knowledge.agent.common.enums.TicketStatusEnum;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class ServiceDeskController {

    /**
     * 服务台流式处理 SSE 超时时间。
     */
    private static final long STREAM_TIMEOUT_MS = 120_000L;

    private final ServiceDeskService serviceDeskService;
    private final TicketService ticketService;
    @Qualifier("aiStreamExecutor")
    private final ThreadPoolTaskExecutor aiStreamExecutor;

    /**
     * 流式提交服务台问题。
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter askStream(@RequestBody ServiceDeskAskReq req) {
        validateQuestion(req);
        Long userId = currentUserId();

        // 先建立 SSE 连接，再异步跑 Agent，避免阻塞请求线程。
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        ServiceDeskSseHandler handler = new ServiceDeskSseHandler(emitter);
        emitter.onTimeout(() -> handler.completeWithError("服务台处理超时，请稍后重试"));
        emitter.onError(error -> handler.markCompleted());
        emitter.onCompletion(handler::markCompleted);

        try {
            aiStreamExecutor.execute(() -> serviceDeskService.askStreamAsUser(toCommand(req), userId, handler));
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

    /**
     * 查询当前用户工单列表。
     */
    @GetMapping("/tickets")
    public Result<PageResponse<ServiceTicketResp>> listTickets(
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "pageSize", defaultValue = "20") long pageSize,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "serviceType", required = false) String serviceType) {
        PageResponse<ServiceTicketResult> tickets = ticketService.listTickets(
                currentUserId(), page, pageSize, TicketStatusEnum.from(status), ServiceTypeEnum.from(serviceType));
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
        return Result.success(ServiceDeskResponseMapper.toTicketResp(serviceDeskService.confirmTicket(id)));
    }

    /**
     * 提交服务台运行反馈。
     */
    @PostMapping("/runs/{runId}/feedback")
    public Result<ServiceDeskFeedbackResp> submitFeedback(@PathVariable("runId") Long runId,
                                                          @RequestBody ServiceDeskFeedbackReq req) {
        if (req == null || req.getResolved() == null) {
            throw new BaseException(ErrorCodeEnum.BAD_REQUEST.getCode(), "反馈结果不能为空");
        }
        ServiceDeskFeedbackResult feedback = serviceDeskService.submitFeedback(
                runId, currentUserId(), req.getResolved(), req.getComment());
        return Result.success(ServiceDeskResponseMapper.toFeedbackResp(feedback));
    }

    /**
     * 校验问题参数。
     */
    private void validateQuestion(ServiceDeskAskReq req) {
        if (req == null || req.getQuestion() == null || req.getQuestion().isBlank()) {
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
        // Controller 只做参数搬运，业务语义交给应用层处理。
        return new ServiceDeskAskCommand(req.getQuestion(), req.getServiceType(), req.getKnowledgeBaseId(), req.getConversationId());
    }

    /**
     * 判断是否为线程池拒绝异常。
     */
    private boolean isTaskRejected(RuntimeException e) {
        return e instanceof org.springframework.core.task.TaskRejectedException
                || e instanceof java.util.concurrent.RejectedExecutionException;
    }
}
