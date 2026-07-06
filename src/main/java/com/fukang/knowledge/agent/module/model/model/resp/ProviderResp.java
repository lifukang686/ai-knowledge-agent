package com.fukang.knowledge.agent.module.model.model.resp;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
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
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProviderResp {
    private Long id;

    private String name;

    private String apiBaseUrl;

    private String apiKey;

    private String description;

    private Boolean isDefault;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

}
