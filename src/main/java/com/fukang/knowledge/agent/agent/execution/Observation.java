package com.fukang.knowledge.agent.agent.execution;

import java.util.Map;

/**
 * 观察结果值对象
 * <p>封装单个步骤执行后的完整信息，供 AgentReasoner 推理使用</p>
 *
 * @param stepOrder    步骤序号
 * @param toolName     工具名称
 * @param parameters   调用参数
 * @param result       执行输出（成功时）
 * @param success      是否执行成功
 * @param durationMs   执行耗时（毫秒）
 * @param errorMessage 错误信息（失败时）
 */
public record Observation(
    Integer stepOrder,
    String toolName,
    Map<String, Object> parameters,
    String result,
    Boolean success,
    Long durationMs,
    String errorMessage
) {

    /**
     * 创建成功的观察结果
     */
    public static Observation success(Integer stepOrder, String toolName,
                                       Map<String, Object> parameters,
                                       String result, long durationMs) {
        return new Observation(stepOrder, toolName, parameters, result, true, durationMs, null);
    }

    /**
     * 创建失败的观察结果
     */
    public static Observation failure(Integer stepOrder, String toolName,
                                       Map<String, Object> parameters,
                                       String errorMessage, long durationMs) {
        return new Observation(stepOrder, toolName, parameters, null, false, durationMs, errorMessage);
    }
}