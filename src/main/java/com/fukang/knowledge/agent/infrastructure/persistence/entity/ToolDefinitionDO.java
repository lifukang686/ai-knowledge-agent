package com.fukang.knowledge.agent.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工具定义实体类
 * <p>对应数据库表 tool_definition，注册 Agent 可调用的工具。
 * 支持 HTTP 接口调用、SQL 数据库查询、本地方法调用三种执行器类型</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "tool_definition")
@TableName("tool_definition")
public class ToolDefinitionDO extends BaseEntity {

    /** 工具名称（唯一标识） */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** 工具功能描述（供 LLM 理解工具用途） */
    @Column(name = "description", columnDefinition = "text")
    private String description;

    /** 执行器类型: HTTP / SQL / LOCAL_METHOD */
    @Column(name = "executor_type", nullable = false, length = 20)
    private String executorType;

    /** 执行器配置（JSON 格式），如 HTTP 的 URL、方法等 */
    @Column(name = "executor_config", columnDefinition = "text")
    private String executorConfig;

    /** 参数 Schema（JSON 格式），供 LLM 理解参数结构 */
    @Column(name = "parameters_schema", columnDefinition = "text")
    private String parametersSchema;

    /** 是否启用: 0=禁用, 1=启用 */
    @Column(name = "enabled")
    private Boolean enabled;
}