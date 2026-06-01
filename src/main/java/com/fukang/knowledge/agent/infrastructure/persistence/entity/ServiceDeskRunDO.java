package com.fukang.knowledge.agent.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 服务台 Agent 单次运行记录。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "service_desk_run")
@TableName("service_desk_run")
public class ServiceDeskRunDO extends BaseEntity {

    /** 发起服务台请求的用户 ID。 */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 用户原始问题。 */
    @Column(name = "question", nullable = false, columnDefinition = "text")
    private String question;

    /** 服务类型：AUTO / IT / HR。 */
    @Column(name = "service_type", length = 20)
    private String serviceType;

    /** 识别出的业务意图。 */
    @Column(name = "intent", length = 40)
    private String intent;

    /** 用户选择的知识库 ID。 */
    @Column(name = "knowledge_base_id")
    private Long knowledgeBaseId;

    /** 关联 RAG 会话 ID。 */
    @Column(name = "conversation_id")
    private Long conversationId;

    /** 最终回答。 */
    @Column(name = "answer", columnDefinition = "text")
    private String answer;

    /** 运行状态：RUNNING / COMPLETED / FAILED。 */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /** 关联工单 ID。 */
    @Column(name = "ticket_id")
    private Long ticketId;

    /** 是否需要用户确认后才执行写操作。 */
    @Column(name = "approval_required", nullable = false)
    private Boolean approvalRequired;

    /** 待确认的草稿工单 ID。 */
    @Column(name = "pending_ticket_id")
    private Long pendingTicketId;

    /** 用户反馈 ID。 */
    @Column(name = "feedback_id")
    private Long feedbackId;

    /** 结构化事件日志 JSON。 */
    @Column(name = "event_log", columnDefinition = "text")
    private String eventLog;

    /** 运行开始时间。 */
    @Column(name = "start_time")
    private LocalDateTime startTime;

    /** 运行结束时间。 */
    @Column(name = "end_time")
    private LocalDateTime endTime;
}
