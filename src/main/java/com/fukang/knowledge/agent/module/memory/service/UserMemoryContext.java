package com.fukang.knowledge.agent.module.memory.service;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fukang.knowledge.agent.module.memory.model.entity.UserMemoryEntity;

import java.util.List;

/**
 * 当前用户可注入 Prompt 的记忆上下文。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserMemoryContext {
    private Long userId;

    private List<UserMemoryEntity> memories;

    private String promptText;

    public boolean hasMemory() {
        return promptText != null && !promptText.isBlank();
    }

}
