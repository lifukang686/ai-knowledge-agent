package com.fukang.knowledge.agent.application.servicedesk.command;

/**
 * 提交服务台运行反馈命令。
 */
public record SubmitFeedbackCommand(
        Long runId,
        Long userId,
        Boolean resolved,
        String comment
) {
}
