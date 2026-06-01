package com.fukang.knowledge.agent.application.servicedesk;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fukang.knowledge.agent.application.servicedesk.command.ConfirmTicketCommand;
import com.fukang.knowledge.agent.application.servicedesk.command.CreateTicketCommand;
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
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.ServiceTicketEventMapper;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.ServiceTicketMapper;
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

    private static final DateTimeFormatter TICKET_NO_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final ServiceTicketMapper ticketMapper;
    private final ServiceTicketEventMapper ticketEventMapper;
    private final ObjectMapper objectMapper;

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
        ticketMapper.insert(ticket);
        writeEvent(ticket.getId(), TicketEventType.DRAFT_CREATED, null, initialStatus,
                command.creatorId(), "已生成工单草稿", Map.of("ticketNo", ticket.getTicketNo()));
        return toResult(ticket);
    }

    public PageResponse<ServiceTicketResult> listTickets(Long userId, long page, long pageSize,
                                                         TicketStatus status, ServiceType serviceType) {
        LambdaQueryWrapper<ServiceTicketDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ServiceTicketDO::getCreatorId, userId);
        if (status != null) {
            wrapper.eq(ServiceTicketDO::getStatus, status.name());
        }
        if (serviceType != null && serviceType != ServiceType.AUTO) {
            wrapper.eq(ServiceTicketDO::getServiceType, serviceType.name());
        }
        wrapper.orderByDesc(ServiceTicketDO::getCreateTime);
        IPage<ServiceTicketDO> resultPage = ticketMapper.selectPage(new Page<>(page, pageSize), wrapper);
        List<ServiceTicketResult> items = resultPage.getRecords().stream().map(this::toResult).toList();
        return new PageResponse<>(items, resultPage.getTotal(), resultPage.getCurrent(), resultPage.getSize());
    }

    public List<ServiceTicketResult> listRecentTickets(Long userId, int limit) {
        LambdaQueryWrapper<ServiceTicketDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ServiceTicketDO::getCreatorId, userId)
                .orderByDesc(ServiceTicketDO::getCreateTime)
                .last("LIMIT " + Math.max(1, limit));
        return ticketMapper.selectList(wrapper).stream().map(this::toResult).toList();
    }

    public ServiceTicketResult getTicket(Long ticketId, Long userId) {
        ServiceTicketDO ticket = ticketMapper.selectById(ticketId);
        if (ticket == null || !userId.equals(ticket.getCreatorId())) {
            throw new BaseException(ErrorCodeEnum.NOT_FOUND.getCode(), "工单不存在");
        }
        return toResult(ticket, true);
    }

    public ServiceTicketResult getTicketByNo(String ticketNo, Long userId) {
        LambdaQueryWrapper<ServiceTicketDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ServiceTicketDO::getTicketNo, ticketNo)
                .eq(ServiceTicketDO::getCreatorId, userId);
        ServiceTicketDO ticket = ticketMapper.selectOne(wrapper);
        return ticket != null ? toResult(ticket) : null;
    }

    @Transactional(rollbackFor = Exception.class)
    public void attachRun(Long ticketId, Long runId) {
        if (ticketId == null || runId == null) {
            return;
        }
        ServiceTicketDO ticket = ticketMapper.selectById(ticketId);
        if (ticket != null) {
            ticket.setSourceRunId(runId);
            ticketMapper.updateById(ticket);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public ServiceTicketResult confirmTicket(ConfirmTicketCommand command) {
        if (command.ticketId() == null || command.userId() == null) {
            throw new BaseException(ErrorCodeEnum.BAD_REQUEST.getCode(), "确认工单参数不完整");
        }
        ServiceTicketDO ticket = ticketMapper.selectById(command.ticketId());
        if (ticket == null || !command.userId().equals(ticket.getCreatorId())) {
            throw new BaseException(ErrorCodeEnum.NOT_FOUND.getCode(), "工单不存在");
        }
        if (!TicketStatus.DRAFT.name().equals(ticket.getStatus())) {
            throw new BaseException(ErrorCodeEnum.BAD_REQUEST.getCode(), "只有草稿工单可以确认");
        }

        TicketStatus fromStatus = TicketStatus.DRAFT;
        ticket.setStatus(TicketStatus.OPEN.name());
        ticketMapper.updateById(ticket);
        writeEvent(ticket.getId(), TicketEventType.CONFIRMED, fromStatus, TicketStatus.OPEN,
                command.userId(), "用户确认草稿，工单已正式打开", Map.of("ticketNo", ticket.getTicketNo()));
        return toResult(ticket, true);
    }

    @Transactional(rollbackFor = Exception.class)
    public void recordHandoffRequested(Long ticketId, Long operatorId, String reason) {
        ServiceTicketDO ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            return;
        }
        writeEvent(ticketId, TicketEventType.HANDOFF_REQUESTED, null, TicketStatus.from(ticket.getStatus()),
                operatorId, "已请求人工介入", Map.of("reason", reason != null ? reason : ""));
    }

    private String generateTicketNo() {
        String time = LocalDateTime.now().format(TICKET_NO_TIME_FORMAT);
        int random = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "T" + time + random;
    }

    private ServiceType resolveServiceType(ServiceType serviceType) {
        return serviceType != null && serviceType != ServiceType.AUTO ? serviceType : ServiceType.IT;
    }

    private TicketPriority resolvePriority(TicketPriority priority) {
        return priority != null ? priority : TicketPriority.MEDIUM;
    }

    private TicketStatus resolveStatus(TicketStatus status) {
        return status != null ? status : TicketStatus.OPEN;
    }

    private String trimToLength(String value, int maxLength, String fallback) {
        String text = StringUtils.hasText(value) ? value.trim() : fallback;
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }

    private ServiceTicketResult toResult(ServiceTicketDO ticket) {
        return toResult(ticket, false);
    }

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

    private List<ServiceTicketEventResult> listTicketEvents(Long ticketId) {
        LambdaQueryWrapper<ServiceTicketEventDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ServiceTicketEventDO::getTicketId, ticketId)
                .orderByAsc(ServiceTicketEventDO::getCreateTime);
        return ticketEventMapper.selectList(wrapper).stream().map(this::toEventResult).toList();
    }

    private Long countTicketEvents(Long ticketId) {
        LambdaQueryWrapper<ServiceTicketEventDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ServiceTicketEventDO::getTicketId, ticketId);
        return ticketEventMapper.selectCount(wrapper);
    }

    private void writeEvent(Long ticketId, TicketEventType eventType, TicketStatus fromStatus, TicketStatus toStatus,
                            Long operatorId, String message, Map<String, Object> payload) {
        ServiceTicketEventDO event = new ServiceTicketEventDO();
        event.setTicketId(ticketId);
        event.setEventType(eventType.name());
        event.setFromStatus(fromStatus != null ? fromStatus.name() : null);
        event.setToStatus(toStatus != null ? toStatus.name() : null);
        event.setOperatorId(operatorId);
        event.setMessage(message);
        event.setPayload(serializePayload(payload));
        ticketEventMapper.insert(event);
    }

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
