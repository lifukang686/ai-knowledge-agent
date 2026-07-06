package com.fukang.knowledge.agent.module.agent.model.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent 执行步骤记录 DTO
 * <p>供 API 返回使用，展示单个步骤的执行概要</p>
 *
 * @param stepOrder    步骤序号
 * @param toolName     工具名称
 * @param observation  工具返回的观察结果
 * @param success      是否成功
 * @param durationMs   耗时（毫秒）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentStepRecord {
    private int stepOrder;

    private String toolName;

    private String observation;

    private boolean success;

    private long durationMs;

}