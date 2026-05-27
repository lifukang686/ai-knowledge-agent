package com.fukang.knowledge.agent.agent.context;

import java.util.Map;

/**
 * Agent 执行步骤记录
 * <p>记录每一步执行后的完整信息，包括工具名、参数、结果、耗时和错误</p>
 *
 * @param stepOrder    步骤序号
 * @param toolName     工具名称
 * @param parameters   调用参数
 * @param observation  工具返回的观察结果
 * @param durationMs   执行耗时（毫秒）
 * @param success      是否执行成功
 * @param errorMessage 错误信息（成功时为空）
 */
public record AgentStep(
    Integer stepOrder,
    String toolName,
    Map<String, Object> parameters,
    String observation,
    Long durationMs,
    Boolean success,
    String errorMessage
) {}