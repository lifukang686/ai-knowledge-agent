package com.fukang.knowledge.agent.module.model.model.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新模型提供商命令，空字段表示不更新。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProviderUpdateCommand {
    private String name;

    private String apiBaseUrl;

    private String apiKey;

    private String description;

}
