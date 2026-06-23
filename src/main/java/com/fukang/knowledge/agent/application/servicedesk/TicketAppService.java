package com.fukang.knowledge.agent.application.servicedesk;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fukang.knowledge.agent.application.servicedesk.command.ConfirmTicketCommand;
import com.fukang.knowledge.agent.application.servicedesk.command.CreateTicketCommand;
import com.fukang.knowledge.agent.application.servicedesk.port.ServiceTicketEventRepository;
import com.fukang.knowledge.agent.application.servicedesk.port.ServiceTicketRepository;
import com.fukang.knowledge.agent.application.servicedesk.result.ServiceTicketEventResult;
import com.fukang.knowledge.agent.application.servicedesk.result.ServiceTicketResult;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.common.result.PageResponse;
import com.fukang.knowledge.agent.domain.servicedesk.ServiceType;
import com.fukang.knowledge.agent.domain.servicedesk.TicketEventType;
import com.fukang.knowledge.agent.domain.servicedesk.TicketPriority;
import com.fukang.knowledge.agent.domain.servicedesk.TicketStatus;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.ServiceTicketEventDO;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.ServiceTicketDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 服务台工单应用服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketAppService {

    /**
     * 工单编号时间部分格式。
     */
    private static final DateTimeFormatter TICKET_NO_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final ServiceTicketRepository serviceTicketRepository;
    private final ServiceTicketEventRepository serviceTicketEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * 创建工单或工单草稿，并记录初始事件。
     */
    @Transactional(rollbackFor = Exception.class)
    public ServiceTicketResult createTicket(CreateTicketCommand command) {
        TicketStatus initialStatus = resolveStatus(command.initialStatus());
        ServiceTicketDO ticket = new ServiceTicketDO();
        ticket.setTicketNo(generateTicketNo());
        ticket.setServiceType(resolveServiceType(command.serviceType()).name());
        ticket.setCategory(StringUtils.hasText(command.category()) ? command.category() : "综合");
        ticket.setPriority(resolvePriority(command.priority()).name());
        ticket.setStatus(initialStatus.name());
        ticket.setTitle(trimToLength(command.title(), 200, "服务台请求"));
        ticket.setDescription(command.description());
        ticket.setAgentSummary(command.agentSummary());
        ticket.setCreatorId(command.creatorId());
        ticket.setSourceRunId(command.sourceRunId());
        ticket.setSourceConversationId(command.sourceConversationId());
        serviceTicketRepository.insert(ticket);
        writeEvent(ticket.getId(), TicketEventType.DRAFT_CREATED, null, initialStatus,
                command.creatorId(), "已生成工单草稿", Map.of("ticketNo", ticket.getTicketNo()));
        return toResult(ticket);
    }

    /**
     * 分页查询当前用户创建的工单。
     */
    public PageResponse<ServiceTicketResult> listTickets(Long userId, long page, long pageSize,
                                                         TicketStatus status, ServiceType serviceType) {
        IPage<ServiceTicketDO> resultPage = serviceTicketRepository.pageByCreator(
                userId, page, pageSize, status, serviceType);
        List<ServiceTicketResult> items = resultPage.getRecords().stream().map(this::toResult).toList();
        return new PageResponse<>(items, resultPage.getTotal(), resultPage.getCurrent(), resultPage.getSize());
    }

    /**
     * 查询当前用户最近工单。
     */
    public List<ServiceTicketResult> listRecentTickets(Long userId, int limit) {
        return serviceTicketRepository.findRecentByCreator(userId, limit).stream().map(this::toResult).toList();
    }

    /**
     * 查询当前用户的单个工单详情。
     */
    public ServiceTicketResult getTicket(Long ticketId, Long userId) {
        ServiceTicketDO ticket = serviceTicketRepository.findById(ticketId);
        if (ticket == null || !userId.equals(ticket.getCreatorId())) {
            throw new BaseException(ErrorCodeEnum.NOT_FOUND.getCode(), "工单不存在");
        }
        return toResult(ticket, true);
    }

    /**
     * 按工单号查询当前用户工单。
     */
    public ServiceTicketResult getTicketByNo(String ticketNo, Long userId) {
        ServiceTicketDO ticket = serviceTicketRepository.findByTicketNoAndCreatorId(ticketNo, userId);
        return ticket != null ? toResult(ticket) : null;
    }

    /**
     * 绑定服务台运行记录。
     */
    @Transactional(rollbackFor = Exception.class)
    public void attachRun(Long ticketId, Long runId) {
        if (ticketId == null || runId == null) {
            return;
        }
        ServiceTicketDO ticket = serviceTicketRepository.findById(ticketId);
        if (ticket != null) {
            ticket.setSourceRunId(runId);
            serviceTicketRepository.updateById(ticket);
        }
    }

    /**
     * 确认 Agent 生成的草稿工单，将状态从 DRAFT 推进到 OPEN。
     */
    @Transactional(rollbackFor = Exception.class)
    public ServiceTicketResult confirmTicket(ConfirmTicketCommand command) {
        if (command.ticketId() == null || command.userId() == null) {
            throw new BaseException(ErrorCodeEnum.BAD_REQUEST.getCode(), "确认工单参数不完整");
        }
        ServiceTicketDO ticket = serviceTicketRepository.findById(command.ticketId());
        if (ticket == null || !command.userId().equals(ticket.getCreatorId())) {
            throw new BaseException(ErrorCodeEnum.NOT_FOUND.getCode(), "工单不存在");
        }
        if (!TicketStatus.DRAFT.name().equals(ticket.getStatus())) {
            throw new BaseException(ErrorCodeEnum.BAD_REQUEST.getCode(), "只有草稿工单可以确认");
        }

        TicketStatus fromStatus = TicketStatus.DRAFT;
        ticket.setStatus(TicketStatus.OPEN.name());
        serviceTicketRepository.updateById(ticket);
        writeEvent(ticket.getId(), TicketEventType.CONFIRMED, fromStatus, TicketStatus.OPEN,
                command.userId(), "用户确认草稿，工单已正式打开", Map.of("ticketNo", ticket.getTicketNo()));
        return toResult(ticket, true);
    }

    /**
     * 记录人工介入事件。
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordHandoffRequested(Long ticketId, Long operatorId, String reason) {
        ServiceTicketDO ticket = serviceTicketRepository.findById(ticketId);
        if (ticket == null) {
            return;
        }
        writeEvent(ticketId, TicketEventType.HANDOFF_REQUESTED, null, TicketStatus.from(ticket.getStatus()),
                operatorId, "已请求人工介入", Map.of("reason", reason != null ? reason : ""));
    }

    /**
     * 生成工单编号。
     */
    private String generateTicketNo() {
        // 时间戳加四位随机数，保证人工可读并降低同秒冲突概率。
        String time = LocalDateTime.now().format(TICKET_NO_TIME_FORMAT);
        int random = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "T" + time + random;
    }

    /**
     * 解析服务类型。
     */
    private ServiceType resolveServiceType(ServiceType serviceType) {
        return serviceType != null && serviceType != ServiceType.AUTO ? serviceType : ServiceType.IT;
    }

    /**
     * 解析工单优先级。
     */
    private TicketPriority resolvePriority(TicketPriority priority) {
        return priority != null ? priority : TicketPriority.MEDIUM;
    }

    /**
     * 解析初始工单状态。
     */
    private TicketStatus resolveStatus(TicketStatus status) {
        return status != null ? status : TicketStatus.OPEN;
    }

    /**
     * 截断文本到指定长度。
     */
    private String trimToLength(String value, int maxLength, String fallback) {
        String text = StringUtils.hasText(value) ? value.trim() : fallback;
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }

    /**
     * 转换工单结果，不包含事件明细。
     */
    private ServiceTicketResult toResult(ServiceTicketDO ticket) {
        return toResult(ticket, false);
    }

    /**
     * 转换工单结果。
     */
    private ServiceTicketResult toResult(ServiceTicketDO ticket, boolean includeEvents) {
        List<ServiceTicketEventResult> events = includeEvents ? listTicketEvents(ticket.getId()) : List.of();
        Long eventCount = includeEvents ? (long) events.size() : countTicketEvents(ticket.getId());
        return new ServiceTicketResult(
                ticket.getId(),
                ticket.getTicketNo(),
                ticket.getServiceType(),
                ticket.getCategory(),
                ticket.getPriority(),
                ticket.getStatus(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getAgentSummary(),
                ticket.getCreatorId(),
                ticket.getAssigneeId(),
                ticket.getSourceRunId(),
                ticket.getSourceConversationId(),
                events,
                eventCount,
                ticket.getCreateTime(),
                ticket.getUpdateTime()
        );
    }

    /**
     * 查询工单事件列表。
     */
    private List<ServiceTicketEventResult> listTicketEvents(Long ticketId) {
        return serviceTicketEventRepository.findByTicketId(ticketId).stream().map(this::toEventResult).toList();
    }

    /**
     * 统计工单事件数量。
     */
    private Long countTicketEvents(Long ticketId) {
        return serviceTicketEventRepository.countByTicketId(ticketId);
    }

    /**
     * 写入工单事件。
     */
    private void writeEvent(Long ticketId, TicketEventType eventType, TicketStatus fromStatus, TicketStatus toStatus,
                            Long operatorId, String message, Map<String, Object> payload) {
        // 工单事件是状态审计来源，状态变化和操作原因都通过事件表沉淀。
        ServiceTicketEventDO event = new ServiceTicketEventDO();
        event.setTicketId(ticketId);
        event.setEventType(eventType.name());
        event.setFromStatus(fromStatus != null ? fromStatus.name() : null);
        event.setToStatus(toStatus != null ? toStatus.name() : null);
        event.setOperatorId(operatorId);
        event.setMessage(message);
        event.setPayload(serializePayload(payload));
        serviceTicketEventRepository.insert(event);
    }

    /**
     * 序列化事件载荷。
     */
    private String serializePayload(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(new LinkedHashMap<>(payload));
        } catch (Exception e) {
            log.warn("服务台工单事件载荷序列化失败", e);
            return "{}";
        }
    }

    /**
     * 转换工单事件结果。
     */
    private ServiceTicketEventResult toEventResult(ServiceTicketEventDO event) {
        return new ServiceTicketEventResult(
                event.getId(),
                event.getTicketId(),
                event.getEventType(),
                event.getFromStatus(),
                event.getToStatus(),
                event.getOperatorId(),
                event.getMessage(),
                event.getPayload(),
                event.getCreateTime()
        );
    }
}
