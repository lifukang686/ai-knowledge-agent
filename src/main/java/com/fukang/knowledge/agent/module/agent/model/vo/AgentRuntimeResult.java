package com.fukang.knowledge.agent.module.agent.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Plan-Execute Agent 单次运行结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentRuntimeResult {
    private String answer;

    private String status;

    private List<AgentStep> steps;

    private List<AgentRunEvent> events;

    private long totalDurationMs;
}
