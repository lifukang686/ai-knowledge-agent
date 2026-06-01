package com.fukang.knowledge.agent.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 服务台工单实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "service_ticket")
@TableName("service_ticket")
public class ServiceTicketDO extends BaseEntity {

    /** 面向用户展示和查询的工单编号。 */
    @Column(name = "ticket_no", nullable = false, length = 40)
    private String ticketNo;

    /** 服务类型：IT / HR。 */
    @Column(name = "service_type", nullable = false, length = 20)
    private String serviceType;

    /** 工单分类，如网络、账号、考勤等。 */
    @Column(name = "category", length = 64)
    private String category;

    /** 工单优先级：LOW / MEDIUM / HIGH / URGENT。 */
    @Column(name = "priority", nullable = false, length = 20)
    private String priority;

    /** 工单状态：DRAFT / OPEN / PROCESSING / RESOLVED / CLOSED。 */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /** 工单标题。 */
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /** 用户原始问题或补充描述。 */
    @Column(name = "description", columnDefinition = "text")
    private String description;

    /** Agent 生成的处理摘要。 */
    @Column(name = "agent_summary", columnDefinition = "text")
    private String agentSummary;

    /** 创建人用户 ID。 */
    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    /** 当前负责人用户 ID，阶段 1 可为空。 */
    @Column(name = "assignee_id")
    private Long assigneeId;

    /** 来源服务台运行 ID。 */
    @Column(name = "source_run_id")
    private Long sourceRunId;

    /** 来源 RAG 会话 ID。 */
    @Column(name = "source_conversation_id")
    private Long sourceConversationId;
}
