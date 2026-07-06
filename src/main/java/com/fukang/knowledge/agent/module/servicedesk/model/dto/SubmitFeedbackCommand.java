package com.fukang.knowledge.agent.module.servicedesk.model.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 提交服务台运行反馈命令。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmitFeedbackCommand {
    private Long runId;

    private Long userId;

    private Boolean resolved;

    private String comment;

}
