package com.fukang.knowledge.agent.api.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 模型配置创建请求 DTO
 *
 * @param providerId   所属提供商ID，不能为空
 * @param modelName    模型名称（如 gpt-3.5-turbo），不能为空
 * @param defaultParams 默认调用参数，JSON 格式字符串，可选
 */
public record ModelConfigReq(
        @NotNull(message = "提供商ID不能为空")
        Long providerId,

        @NotBlank(message = "模型名称不能为空")
        String modelName,

        String defaultParams
) {}
