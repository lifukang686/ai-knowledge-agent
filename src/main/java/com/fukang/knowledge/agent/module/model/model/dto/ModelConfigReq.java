package com.fukang.knowledge.agent.module.model.model.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 模型配置创建请求 DTO
 *
 * @param providerId    所属提供商ID，不能为空
 * @param modelName     模型名称（如 gpt-3.5-turbo），不能为空
 * @param modelType     模型类型（CHAT/EMBEDDING/RERANK/STT），不能为空
 * @param defaultParams 默认调用参数，JSON 格式字符串，可选
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelConfigReq {
    @NotNull(message = "提供商ID不能为空")
    private Long providerId;

    @NotBlank(message = "模型名称不能为空")
    private String modelName;

    @NotBlank(message = "模型类型不能为空")
    private String modelType;

    private String defaultParams;

}
