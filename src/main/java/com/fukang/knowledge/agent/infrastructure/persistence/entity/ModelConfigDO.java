package com.fukang.knowledge.agent.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 模型配置实体类
 * <p>对应数据库表 model_config，存储具体 AI 模型的配置信息，
 * 每个配置归属于一个模型提供商</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("model_config")
public class ModelConfigDO extends BaseEntity {

    /** 所属提供商ID，关联 model_provider 表 */
    private Long providerId;

    /** 模型名称（如 gpt-3.5-turbo、gpt-4） */
    private String modelName;

    /** 默认调用参数，JSON 格式字符串（如 temperature、max_tokens 等） */
    private String defaultParams;
}
