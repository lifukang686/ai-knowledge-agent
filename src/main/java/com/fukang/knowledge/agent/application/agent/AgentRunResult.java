package com.fukang.knowledge.agent.application.agent;

import java.util.List;

/**
 * Agent 运行结果 DTO
 * <p>API 层返回的 Agent 执行结果，包含运行基本信息、步骤详情和总耗时</p>
 *
 * @param runId           运行记录 ID
 * @param result          最终回答文本
 * @param status          运行状态: PLANNING / EXECUTING / COMPLETED / FAILED
 * @param steps           执行步骤记录列表
 * @param totalDurationMs 总执行耗时（毫秒）
 */
public record AgentRunResult(
    Long runId,
    String result,
    String status,
    List<AgentStepRecord> steps,
    Long totalDurationMs
) {}