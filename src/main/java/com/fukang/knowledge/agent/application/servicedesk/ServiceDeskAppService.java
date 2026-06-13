package com.fukang.knowledge.agent.application.servicedesk;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fukang.knowledge.agent.application.servicedesk.agent.ServiceDeskAgentRuntime;
import com.fukang.knowledge.agent.application.servicedesk.command.ConfirmTicketCommand;
import com.fukang.knowledge.agent.application.servicedesk.command.ServiceDeskAskCommand;
import com.fukang.knowledge.agent.application.servicedesk.command.SubmitFeedbackCommand;
import com.fukang.knowledge.agent.application.servicedesk.port.ServiceDeskFeedbackRepository;
import com.fukang.knowledge.agent.application.servicedesk.port.ServiceDeskRunRepository;
import com.fukang.knowledge.agent.application.servicedesk.result.ServiceDeskAnswerResult;
import com.fukang.knowledge.agent.application.servicedesk.result.ServiceDeskFeedbackResult;
import com.fukang.knowledge.agent.application.servicedesk.result.ServiceDeskRunResult;
import com.fukang.knowledge.agent.application.servicedesk.result.ServiceTicketResult;
import com.fukang.knowledge.agent.common.context.UserContextHolder;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.domain.agent.model.AgentRunEvent;
import com.fukang.knowledge.agent.domain.servicedesk.ServiceType;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.ServiceDeskFeedbackDO;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.ServiceDeskRunDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 企业服务台 Agent 应用服务。
 * <p>业务入口保持稳定，内部通过受控 Plan-Execute Runtime 让 Agent 规划并调用服务台工具。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceDeskAppService {

    /**
     * 工单应用服务。
     */
    private final TicketAppService ticketAppService;

    /**
     * 服务台 Agent 运行时。
     */
    private final ServiceDeskAgentRuntime serviceDeskAgentRuntime;

    /**
     * 服务台运行记录仓储。
     */
    private final ServiceDeskRunRepository serviceDeskRunRepository;

    /**
     * 服务台反馈仓储。
     */
    private final ServiceDeskFeedbackRepository serviceDeskFeedbackRepository;

    /**
     * JSON 序列化工具。
     */
    private final ObjectMapper objectMapper;

    /**
     * 指定用户上下文执行流式服务台 Agent，供 Controller 的异步线程复用。
     */
    public void askStreamAsUser(ServiceDeskAskCommand command, Long userId, ServiceDeskStreamHandler handler) {
        if (userId == null) {
            throw new BaseException(ErrorCodeEnum.UNAUTHORIZED);
        }
        UserContextHolder.setUserId(userId);
        if (!StringUtils.hasText(command.question())) {
            throw new BaseException(ErrorCodeEnum.BAD_REQUEST.getCode(), "问题不能为空");
        }

        ServiceDeskRunDO run = null;
        try {
            run = createRun(userId, command);
            stage(handler, "agent_start", "服务台 Agent 正在规划处理步骤");
            ServiceDeskAnswerResult result = serviceDeskAgentRuntime.run(command, userId, run.getId());
            List<AgentRunEvent> events = completeRun(run, result, result.events());
            token(handler, result.answer());
            handler.onDone(result.withEvents(List.copyOf(events)));
        } catch (Exception e) {
            log.error("服务台流式处理失败", e);
            if (run != null) {
                failRun(run, List.of(), e);
            }
            handler.onError("服务台处理失败，请稍后重试", e);
        } finally {
            UserContextHolder.clear();
        }
    }

    /**
     * 确认服务台工单。
     */
    @Transactional(rollbackFor = Exception.class)
    public ServiceTicketResult confirmTicket(Long ticketId) {
        Long userId = currentUserId();
        ServiceTicketResult ticket = ticketAppService.confirmTicket(new ConfirmTicketCommand(ticketId, userId));
        // Agent 只创建草稿；用户确认后，运行记录才绑定正式打开的工单。
        if (ticket.sourceRunId() != null) {
            ServiceDeskRunDO run = serviceDeskRunRepository.findById(ticket.sourceRunId());
            if (run != null && userId.equals(run.getUserId())) {
                run.setApprovalRequired(false);
                run.setPendingTicketId(null);
                run.setTicketId(ticket.id());
                serviceDeskRunRepository.updateById(run);
            }
        }
        return ticket;
    }

    /**
     * 提交服务台处理反馈，同一次运行同一用户只能提交一次。
     */
    @Transactional(rollbackFor = Exception.class)
    public ServiceDeskFeedbackResult submitFeedback(SubmitFeedbackCommand command) {
        if (command.resolved() == null) {
            throw new BaseException(ErrorCodeEnum.BAD_REQUEST.getCode(), "反馈结果不能为空");
        }
        ServiceDeskRunDO run = serviceDeskRunRepository.findById(command.runId());
        if (run == null || !command.userId().equals(run.getUserId())) {
            throw new BaseException(ErrorCodeEnum.NOT_FOUND.getCode(), "服务台运行记录不存在");
        }
        ServiceDeskFeedbackDO existing = findFeedback(command.runId(), command.userId());
        if (existing != null) {
            throw new BaseException(ErrorCodeEnum.BAD_REQUEST.getCode(), "该次服务台处理已提交过反馈");
        }

        ServiceDeskFeedbackDO feedback = new ServiceDeskFeedbackDO();
        feedback.setRunId(command.runId());
        feedback.setTicketId(run.getTicketId());
        feedback.setResolved(command.resolved());
        feedback.setComment(trimComment(command.comment()));
        feedback.setUserId(command.userId());
        serviceDeskFeedbackRepository.insert(feedback);

        run.setFeedbackId(feedback.getId());
        serviceDeskRunRepository.updateById(run);
        return toFeedbackResult(feedback);
    }

    /**
     * 创建服务台运行记录。
     */
    private ServiceDeskRunDO createRun(Long userId, ServiceDeskAskCommand command) {
        ServiceDeskRunDO run = new ServiceDeskRunDO();
        run.setUserId(userId);
        run.setQuestion(command.question());
        run.setServiceType(ServiceType.from(command.serviceType()).name());
        run.setKnowledgeBaseId(command.knowledgeBaseId());
        run.setConversationId(command.conversationId());
        run.setStatus("RUNNING");
        run.setApprovalRequired(false);
        run.setStartTime(LocalDateTime.now());
        serviceDeskRunRepository.insert(run);
        return run;
    }

    /**
     * 完成运行并保存结果。
     */
    private List<AgentRunEvent> completeRun(ServiceDeskRunDO run, ServiceDeskAnswerResult result,
                                            List<AgentRunEvent> events) {
        // Runtime 返回的事件可能是不可变列表，落库前统一复制，避免追加收尾事件时抛异常。
        List<AgentRunEvent> mutableEvents = new ArrayList<>(events != null ? events : List.of());
        mutableEvents.add(event(AgentRunEvent.EventType.FINAL_ANSWER, "服务台处理完成", Map.of(
                "answer", result.answer(),
                "status", result.status(),
                "ticketNo", result.ticketNo() != null ? result.ticketNo() : "",
                "approvalRequired", Boolean.TRUE.equals(result.approvalRequired())
        )));
        run.setAnswer(result.answer());
        run.setStatus("COMPLETED");
        run.setIntent(result.intent());
        run.setServiceType(result.serviceType());
        run.setTicketId(result.ticketId());
        run.setConversationId(result.conversationId());
        run.setApprovalRequired(Boolean.TRUE.equals(result.approvalRequired()));
        run.setPendingTicketId(result.pendingTicket() != null ? result.pendingTicket().id() : null);
        run.setEventLog(serializeEvents(mutableEvents));
        run.setEndTime(LocalDateTime.now());
        serviceDeskRunRepository.updateById(run);
        return mutableEvents;
    }

    /**
     * 标记运行失败。
     */
    private void failRun(ServiceDeskRunDO run, List<AgentRunEvent> events, Exception e) {
        log.error("服务台 Agent 运行失败: runId={}", run.getId(), e);
        // 异常处理也可能收到 List.of()，这里同样做防御性复制，确保错误能被正常记录。
        List<AgentRunEvent> mutableEvents = new ArrayList<>(events != null ? events : List.of());
        mutableEvents.add(event(AgentRunEvent.EventType.ERROR, "服务台处理失败", Map.of(
                "exception", e.getClass().getSimpleName(),
                "message", e.getMessage() != null ? e.getMessage() : ""
        )));
        run.setStatus("FAILED");
        run.setAnswer(e.getMessage());
        run.setEventLog(serializeEvents(mutableEvents));
        run.setEndTime(LocalDateTime.now());
        serviceDeskRunRepository.updateById(run);
    }

    /**
     * 创建运行事件。
     */
    private AgentRunEvent event(AgentRunEvent.EventType type, String message, Map<String, Object> payload) {
        return AgentRunEvent.of(type, null, null, payload, true, null, message);
    }

    /**
     * 序列化运行事件。
     */
    private String serializeEvents(List<AgentRunEvent> events) {
        try {
            return objectMapper.writeValueAsString(events);
        } catch (Exception e) {
            log.error("服务台事件序列化失败", e);
            return "[]";
        }
    }

    /**
     * 查询用户反馈。
     */
    private ServiceDeskFeedbackDO findFeedback(Long runId, Long userId) {
        return serviceDeskFeedbackRepository.findByRunIdAndUserId(runId, userId);
    }

    /**
     * 转换反馈结果。
     */
    private ServiceDeskFeedbackResult toFeedbackResult(ServiceDeskFeedbackDO feedback) {
        return new ServiceDeskFeedbackResult(feedback.getId(), feedback.getRunId(), feedback.getTicketId(),
                feedback.getResolved(), feedback.getComment(), feedback.getUserId(), feedback.getCreateTime());
    }

    /**
     * 裁剪反馈备注。
     */
    private String trimComment(String comment) {
        if (!StringUtils.hasText(comment)) {
            return null;
        }
        String trimmed = comment.trim();
        return trimmed.length() > 1000 ? trimmed.substring(0, 1000) : trimmed;
    }

    /**
     * 获取当前用户 ID。
     */
    private Long currentUserId() {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new BaseException(ErrorCodeEnum.UNAUTHORIZED);
        }
        return userId;
    }

    /**
     * 发送阶段消息。
     */
    private void stage(ServiceDeskStreamHandler handler, String stage, String message) {
        if (handler != null) {
            handler.onStage(stage, message);
        }
    }

    /**
     * 发送文本片段。
     */
    private void token(ServiceDeskStreamHandler handler, String text) {
        if (handler != null && text != null) {
            handler.onToken(text);
        }
    }

}
