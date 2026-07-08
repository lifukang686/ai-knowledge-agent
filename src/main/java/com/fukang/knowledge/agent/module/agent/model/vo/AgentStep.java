package com.fukang.knowledge.agent.module.agent.model.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

/**
 * Agent 执行步骤记录
 * <p>记录每一步执行后的完整信息，包括工具名、参数、结果、耗时和错误</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentStep {
    /** 步骤序号 */
    private Integer stepOrder;

    /** 工具名称 */
    private String toolName;

    /** 调用参数 */
    private Map<String, Object> parameters;

    /** 工具返回的观察结果 */
    private String observation;

    /** 执行耗时（毫秒） */
    private Long durationMs;

    /** 是否执行成功 */
    private Boolean success;

    /** 错误信息（成功时为空） */
    private String errorMessage;

}