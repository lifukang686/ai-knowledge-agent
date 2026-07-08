package com.fukang.knowledge.agent.module.agent.model.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工具执行结果
 * <p>封装工具执行的成功/失败状态、输出内容和耗时，供 Agent 推理引擎使用</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolExecutionResult {
    /** 是否执行成功 */
    private boolean success;

    /** 执行输出（成功时为工具返回内容） */
    private String output;

    /** 错误信息（失败时填充） */
    private String errorMessage;

    /** 执行耗时（毫秒） */
    private long durationMs;

    /**
     * 创建成功的执行结果
     *
     * @param output     工具返回内容
     * @param durationMs 执行耗时
     * @return 成功的执行结果
     */
    public static ToolExecutionResult success(String output, long durationMs) {
        return new ToolExecutionResult(true, output, null, durationMs);
    }

    /**
     * 创建失败的执行结果
     *
     * @param errorMessage 错误信息
     * @param durationMs   执行耗时
     * @return 失败的执行结果
     */
    public static ToolExecutionResult failure(String errorMessage, long durationMs) {
        return new ToolExecutionResult(false, null, errorMessage, durationMs);
    }

}