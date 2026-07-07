package com.fukang.knowledge.agent.module.servicedesk.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fukang.knowledge.agent.module.servicedesk.model.dto.CreateTicketCommand;
import com.fukang.knowledge.agent.module.servicedesk.mapper.ServiceTicketEventMapper;
import com.fukang.knowledge.agent.module.servicedesk.mapper.ServiceTicketMapper;
import com.fukang.knowledge.agent.module.servicedesk.model.vo.ServiceTicketEventResult;
import com.fukang.knowledge.agent.module.servicedesk.model.vo.ServiceTicketResult;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.common.result.PageResponse;
import com.fukang.knowledge.agent.common.enums.ServiceTypeEnum;
import com.fukang.knowledge.agent.common.enums.TicketEventTypeEnum;
import com.fukang.knowledge.agent.common.enums.TicketPriorityEnum;
import com.fukang.knowledge.agent.common.enums.TicketStatusEnum;
import com.fukang.knowledge.agent.module.servicedesk.model.entity.ServiceTicketEventEntity;
import com.fukang.knowledge.agent.module.servicedesk.model.entity.ServiceTicketEntity;
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
public class TicketService {

    /**
     * 工单编号时间部分格式。
     */
    private static final DateTimeFormatter TICKET_NO_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final ServiceTicketMapper serviceTicketMapper;
    private final ServiceTicketEventMapper serviceTicketEventMapper;
    private final ObjectMapper objectMapper;

    /**
     * 创建工单或工单草稿，并记录初始事件。
     */
    @Transactional(rollbackFor = Exception.class)
    public ServiceTicketResult createTicket(CreateTicketCommand command) {
        TicketStatusEnum initialStatus = resolveStatus(command.getInitialStatus());
        ServiceTicketEntity ticket = new ServiceTicketEntity();
        ticket.setTicketNo(generateTicketNo());
        ticket.setServiceType(resolveServiceType(command.getServiceType()).name());
        ticket.setCategory(StringUtils.hasText(command.getCategory()) ? command.getCategory() : "综合");
        ticket.setPriority(resolvePriority(command.getPriority()).name());
        ticket.setStatus(initialStatus.name());
        ticket.setTitle(trimToLength(command.getTitle(), 200, "服务台请求"));
        ticket.setDescription(command.getDescription());
        ticket.setAgentSummary(command.getAgentSummary());
        ticket.setCreatorId(command.getCreatorId());
        ticket.setSourceRunId(command.getSourceRunId());
        ticket.setSourceConversationId(command.getSourceConversationId());
        serviceTicketMapper.insert(ticket);
        writeEvent(ticket.getId(), TicketEventTypeEnum.DRAFT_CREATED, null, initialStatus,
                command.getCreatorId(), "已生成工单草稿", Map.of("ticketNo", ticket.getTicketNo()));
        return toResult(ticket);
    }

    /**
     * 分页查询当前用户创建的工单。
     */
    public PageResponse<ServiceTicketResult> listTickets(Long userId, long page, long pageSize,
                                                         TicketStatusEnum status, ServiceTypeEnum serviceType) {
        IPage<ServiceTicketEntity> resultPage = serviceTicketMapper.pageByCreator(
                userId, page, pageSize, status, serviceType);
        List<ServiceTicketResult> items = resultPage.getRecords().stream().map(this::toResult).toList();
        return new PageResponse<>(items, resultPage.getTotal(), resultPage.getCurrent(), resultPage.getSize());
    }

    /**
     * 查询当前用户最近工单。
     */
    public List<ServiceTicketResult> listRecentTickets(Long userId, int limit) {
        return serviceTicketMapper.findRecentByCreator(userId, limit).stream().map(this::toResult).toList();
    }

    /**
     * 按工单号查询当前用户工单。
     */
    public ServiceTicketResult getTicketByNo(String ticketNo, Long userId) {
        ServiceTicketEntity ticket = serviceTicketMapper.findByTicketNoAndCreatorId(ticketNo, userId);
        return ticket != null ? toResult(ticket) : null;
    }

    /**
     * 确认 Agent 生成的草稿工单，将状态从 DRAFT 推进到 OPEN。
     */
    @Transactional(rollbackFor = Exception.class)
    public ServiceTicketResult confirmTicket(Long ticketId, Long userId) {
        if (ticketId == null || userId == null) {
            throw new BaseException(ErrorCodeEnum.BAD_REQUEST.getCode(), "确认工单参数不完整");
        }
        ServiceTicketEntity ticket = serviceTicketMapper.selectById(ticketId);
        if (ticket == null || !userId.equals(ticket.getCreatorId())) {
            throw new BaseException(ErrorCodeEnum.NOT_FOUND.getCode(), "工单不存在");
        }
        if (!TicketStatusEnum.DRAFT.name().equals(ticket.getStatus())) {
            throw new BaseException(ErrorCodeEnum.BAD_REQUEST.getCode(), "只有草稿工单可以确认");
        }

        TicketStatusEnum fromStatus = TicketStatusEnum.DRAFT;
        ticket.setStatus(TicketStatusEnum.OPEN.name());
        serviceTicketMapper.updateById(ticket);
        writeEvent(ticket.getId(), TicketEventTypeEnum.CONFIRMED, fromStatus, TicketStatusEnum.OPEN,
                userId, "用户确认草稿，工单已正式打开", Map.of("ticketNo", ticket.getTicketNo()));
        return toResult(ticket, true);
    }

    /**
     * 记录人工介入事件。
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordHandoffRequested(Long ticketId, Long operatorId, String reason) {
        ServiceTicketEntity ticket = serviceTicketMapper.selectById(ticketId);
        if (ticket == null) {
            return;
        }
        writeEvent(ticketId, TicketEventTypeEnum.HANDOFF_REQUESTED, null, TicketStatusEnum.from(ticket.getStatus()),
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
    private ServiceTypeEnum resolveServiceType(ServiceTypeEnum serviceType) {
        return serviceType != null && serviceType != ServiceTypeEnum.AUTO ? serviceType : ServiceTypeEnum.IT;
    }

    /**
     * 解析工单优先级。
     */
    private TicketPriorityEnum resolvePriority(TicketPriorityEnum priority) {
        return priority != null ? priority : TicketPriorityEnum.MEDIUM;
    }

    /**
     * 解析初始工单状态。
     */
    private TicketStatusEnum resolveStatus(TicketStatusEnum status) {
        return status != null ? status : TicketStatusEnum.OPEN;
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
    private ServiceTicketResult toResult(ServiceTicketEntity ticket) {
        return toResult(ticket, false);
    }

    /**
     * 转换工单结果。
     */
    private ServiceTicketResult toResult(ServiceTicketEntity ticket, boolean includeEvents) {
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
        return serviceTicketEventMapper.findByTicketId(ticketId).stream().map(this::toEventResult).toList();
    }

    /**
     * 统计工单事件数量。
     */
    private Long countTicketEvents(Long ticketId) {
        return serviceTicketEventMapper.countByTicketId(ticketId);
    }

    /**
     * 写入工单事件。
     */
    private void writeEvent(Long ticketId, TicketEventTypeEnum eventType, TicketStatusEnum fromStatus, TicketStatusEnum toStatus,
                            Long operatorId, String message, Map<String, Object> payload) {
        // 工单事件是状态审计来源，状态变化和操作原因都通过事件表沉淀。
        ServiceTicketEventEntity event = new ServiceTicketEventEntity();
        event.setTicketId(ticketId);
        event.setEventType(eventType.name());
        event.setFromStatus(fromStatus != null ? fromStatus.name() : null);
        event.setToStatus(toStatus != null ? toStatus.name() : null);
        event.setOperatorId(operatorId);
        event.setMessage(message);
        event.setPayload(serializePayload(payload));
        serviceTicketEventMapper.insert(event);
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
    private ServiceTicketEventResult toEventResult(ServiceTicketEventEntity event) {
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
