package com.fukang.knowledge.agent.module.agent.model.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

/**
 * 观察结果值对象
 * <p>封装单个步骤执行后的完整信息，供 AgentReasoner 推理使用</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Observation {
    /** 步骤序号 */
    private Integer stepOrder;

    /** 工具名称 */
    private String toolName;

    /** 调用参数 */
    private Map<String, Object> parameters;

    /** 执行输出（成功时） */
    private String result;

    /** 是否执行成功 */
    private Boolean success;

    /** 执行耗时（毫秒） */
    private Long durationMs;

    /** 错误信息（失败时） */
    private String errorMessage;

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