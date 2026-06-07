package com.fukang.knowledge.agent.application.memory;

import com.fukang.knowledge.agent.infrastructure.persistence.entity.UserMemoryDO;

import java.util.List;

/**
 * 当前用户可注入 Prompt 的记忆上下文。
 */
public record UserMemoryContext(
        Long userId,
        List<UserMemoryDO> memories,
        String promptText
) {
    public boolean hasMemory() {
        return promptText != null && !promptText.isBlank();
    }
}
