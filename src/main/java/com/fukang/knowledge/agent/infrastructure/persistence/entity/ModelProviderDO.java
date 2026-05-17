package com.fukang.knowledge.agent.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 模型提供商实体类
 * <p>对应数据库表 model_provider，存储 AI 模型提供商（如 OpenAI、Azure）的连接配置信息</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "model_provider")
@TableName("model_provider")
public class ModelProviderDO extends BaseEntity {

    /** 提供商名称（如 OpenAI、Azure、Anthropic） */
    @Column(name = "name", nullable = false, unique = true, length = 64)
    private String name;

    /** 提供商 API 基础地址 */
    @Column(name = "api_base_url", length = 255)
    private String apiBaseUrl;

    /** 提供商 API 密钥 */
    @Column(name = "api_key", length = 255)
    private String apiKey;

    /** 提供商描述信息 */
    @Column(name = "description", length = 500)
    private String description;

    /** 是否为默认提供商，系统中只能存在一个默认提供商 */
    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;
}
