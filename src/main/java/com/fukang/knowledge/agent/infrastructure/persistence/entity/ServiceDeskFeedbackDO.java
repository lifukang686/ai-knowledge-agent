package com.fukang.knowledge.agent.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 服务台用户反馈实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "service_desk_feedback")
@TableName("service_desk_feedback")
public class ServiceDeskFeedbackDO extends BaseEntity {

    /** 关联服务台运行 ID。 */
    @Column(name = "run_id", nullable = false)
    private Long runId;

    /** 关联工单 ID，可为空。 */
    @Column(name = "ticket_id")
    private Long ticketId;

    /** 用户是否认为问题已解决。 */
    @Column(name = "resolved", nullable = false)
    private Boolean resolved;

    /** 用户补充备注。 */
    @Column(name = "comment", columnDefinition = "text")
    private String comment;

    /** 反馈提交人。 */
    @Column(name = "user_id", nullable = false)
    private Long userId;
}
