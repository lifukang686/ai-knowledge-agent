package com.fukang.knowledge.agent.domain.agent.model;

import java.time.LocalDateTime;
import java.util.Map;

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

    public static AgentRunEvent of(EventType type, Integer stepOrder, String toolName,
                                   Map<String, Object> payload, Boolean success,
                                   Long durationMs, String message) {
        return new AgentRunEvent(type.code(), stepOrder, toolName, payload,
                success, durationMs, message, LocalDateTime.now());
    }

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
