package com.fukang.knowledge.agent.api.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 模型提供商响应 DTO。
 *
 * @param id          提供商 ID
 * @param name        提供商名称
 * @param apiBaseUrl  API 基础地址
 * @param apiKey      脱敏后的 API 密钥
 * @param description 描述
 * @param isDefault   是否默认
 * @param createTime  创建时间
 * @param updateTime  更新时间
 */
public record ProviderResp(
        Long id,
        String name,
        String apiBaseUrl,
        String apiKey,
        String description,
        Boolean isDefault,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        LocalDateTime createTime,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        LocalDateTime updateTime
) {
}
