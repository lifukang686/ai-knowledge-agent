package com.fukang.knowledge.agent.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fukang.knowledge.agent.infrastructure.persistence.handler.JsonTypeHandler;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import com.fukang.knowledge.agent.common.enums.ModelTypeEnum;

/**
 * 模型配置实体类
 * <p>对应数据库表 model_config，存储具体 AI 模型的配置信息，
 * 每个配置归属于一个模型提供商</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "model_config")
@TableName(value = "model_config", autoResultMap = true)
public class ModelConfigDO extends BaseEntity {

    /** 所属提供商ID，关联 model_provider 表 */
    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    /** 模型名称（如 gpt-3.5-turbo、gpt-4） */
    @Column(name = "model_name", nullable = false, length = 64)
    private String modelName;

    /** 模型类型（CHAT-对话模型、EMBEDDING-嵌入模型、RERANK-重排序模型、STT-语音转文字） */
    @Column(name = "model_type", nullable = false, length = 32)
    private String modelType;

    /** 默认调用参数，JSON 格式字符串（如 temperature、max_tokens 等） */
    @Column(name = "default_params", columnDefinition = "json")
    @TableField(typeHandler = JsonTypeHandler.class)
    private String defaultParams;

    public ModelTypeEnum getModelTypeEnum() {
        return modelType != null ? ModelTypeEnum.fromCode(modelType) : null;
    }

    public void setModelTypeEnum(ModelTypeEnum type) {
        this.modelType = type != null ? type.getCode() : null;
    }
}
