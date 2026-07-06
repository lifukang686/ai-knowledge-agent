package com.fukang.knowledge.agent.module.model.model.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新模型配置命令，空字段表示不更新。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelConfigUpdateCommand {
    private Long providerId;

    private String modelName;

    private String modelType;

    private String defaultParams;

}
