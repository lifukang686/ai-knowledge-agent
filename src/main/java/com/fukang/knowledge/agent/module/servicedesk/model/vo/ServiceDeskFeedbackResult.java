package com.fukang.knowledge.agent.module.servicedesk.model.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 服务台用户反馈结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceDeskFeedbackResult {
    /**
     * 反馈记录 ID。
     */
    private Long id;

    /**
     * 关联服务台运行记录 ID。
     */
    private Long runId;

    /**
     * 关联工单 ID。
     */
    private Long ticketId;

    /**
     * 用户反馈问题是否已解决。
     */
    private Boolean resolved;

    /**
     * 用户反馈备注。
     */
    private String comment;

    /**
     * 提交反馈的用户 ID。
     */
    private Long userId;

    /**
     * 反馈创建时间。
     */
    private LocalDateTime createTime;

}
