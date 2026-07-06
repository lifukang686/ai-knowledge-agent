package com.fukang.knowledge.agent.module.model.model.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;

/**
 * 模型提供商创建请求 DTO
 *
 * @param name       提供商名称（如 OpenAI、Azure），不能为空
 * @param apiBaseUrl 提供商 API 基础地址，可选
 * @param apiKey     提供商 API 密钥，可选
 * @param description 提供商描述信息，可选
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProviderReq {
    @NotBlank(message = "提供商名称不能为空")
    private String name;

    private String apiBaseUrl;

    private String apiKey;

    private String description;

}
