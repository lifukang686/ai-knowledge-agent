package com.fukang.knowledge.agent.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 模型提供商实体类
 * <p>对应数据库表 model_provider，存储 AI 模型提供商（如 OpenAI、Azure）的连接配置信息</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("model_provider")
public class ModelProviderDO extends BaseEntity {

    /** 提供商名称（如 OpenAI、Azure、Anthropic） */
    private String name;

    /** 提供商 API 基础地址 */
    private String apiBaseUrl;

    /** 提供商 API 密钥 */
    private String apiKey;

    /** 提供商描述信息 */
    private String description;
}
