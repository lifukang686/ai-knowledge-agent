package com.fukang.knowledge.agent.domain.agent.model;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Agent 运行事件。
 * <p>Plan-Execute 与 AiServices 两条链路统一写入该结构，前端运行详情页可按 type 渲染。</p>
 *
 * @param type       事件类型：plan/tool_call/observation/reasoning/final_answer/error
 * @param stepOrder  关联计划步骤序号，非步骤事件可为空
 * @param toolName   关联工具名称，仅工具调用/观察事件需要
 * @param payload    事件结构化载荷，如计划步骤、工具参数、工具输出等
 * @param success    执行是否成功；不适用时为空
 * @param durationMs 工具调用耗时；不适用时为空
 * @param message    给前端展示的简短说明
 * @param timestamp  事件产生时间
 */
public record AgentRunEvent(
    String type,
    Integer stepOrder,
    String toolName,
    Map<String, Object> payload,
    Boolean success,
    Long durationMs,
    String message,
    LocalDateTime timestamp
) {

    /** 统一事件创建入口，避免调用方忘记补 timestamp。 */
    public static AgentRunEvent of(EventType type, Integer stepOrder, String toolName,
                                   Map<String, Object> payload, Boolean success,
                                   Long durationMs, String message) {
        return new AgentRunEvent(type.code(), stepOrder, toolName, payload,
                success, durationMs, message, LocalDateTime.now());
    }

    /** 前端和持久化日志共同使用的稳定事件类型编码。 */
    public enum EventType {
        PLAN("plan"),
        TOOL_CALL("tool_call"),
        OBSERVATION("observation"),
        REASONING("reasoning"),
        FINAL_ANSWER("final_answer"),
        ERROR("error");

        private final String code;

        EventType(String code) {
            this.code = code;
        }

        public String code() {
            return code;
        }
    }
}
