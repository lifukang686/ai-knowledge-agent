package com.fukang.knowledge.agent.module.model.model.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 模型配置响应对象。
 * Controller 使用响应 DTO 隔离持久化实体，避免把 MyBatis/JPA 实体直接暴露给前端。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelConfigResp {
    private Long id;

    private Long providerId;

    private String modelName;

    private String modelType;

    private String defaultParams;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}
