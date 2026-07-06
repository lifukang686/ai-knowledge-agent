package com.fukang.knowledge.agent.module.model.model.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建模型配置命令。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelConfigCommand {
    private Long providerId;

    private String modelName;

    private String modelType;

    private String defaultParams;

}
