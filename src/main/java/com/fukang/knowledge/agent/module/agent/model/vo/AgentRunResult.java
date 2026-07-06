package com.fukang.knowledge.agent.module.agent.model.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
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
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentRunResult {
    private Long runId;

    private String result;

    private String status;

    private List<AgentStepRecord> steps;

    private List<AgentRunEvent> events;

    private Long totalDurationMs;

}
