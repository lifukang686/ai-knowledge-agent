package com.fukang.knowledge.agent.module.model.model.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建模型提供商命令。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProviderCommand {
    private String name;

    private String apiBaseUrl;

    private String apiKey;

    private String description;

}
