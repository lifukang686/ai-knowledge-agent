package com.fukang.knowledge.agent.module.model.model.resp;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 模型提供商响应 DTO。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProviderResp {
    /** 提供商 ID */
    private Long id;

    /** 提供商名称 */
    private String name;

    /** API 基础地址 */
    private String apiBaseUrl;

    /** 脱敏后的 API 密钥 */
    private String apiKey;

    /** 描述 */
    private String description;

    /** 是否默认 */
    private Boolean isDefault;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

}
