package com.fukang.knowledge.agent.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fukang.knowledge.agent.infrastructure.persistence.handler.JsonTypeHandler;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Agent 配置实体类
 * <p>对应数据库表 agent，定义智能体的基本配置，
 * 包括名称、描述、关联工具集和系统提示词模板</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "agent")
@TableName(value = "agent", autoResultMap = true)
public class AgentDO extends BaseEntity {

    /** Agent 名称 */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** Agent 功能描述 */
    @Column(name = "description", columnDefinition = "text")
    private String description;

    /** 关联工具ID列表（JSON数组），如 [1,2,3] */
    @Column(name = "tool_ids", columnDefinition = "json")
    @TableField(typeHandler = JsonTypeHandler.class)
    private String toolIds;

    /** 系统提示词模板，可为空则使用默认模板 */
    @Column(name = "system_prompt", columnDefinition = "text")
    private String systemPrompt;

    /** 最大执行步数，防止无限循环，默认 10 */
    @Column(name = "max_steps")
    private Integer maxSteps;

    /**
     * 执行策略：PLAN_EXECUTE（默认，复杂多步任务）/ AI_SERVICES（LLM 原生工具调用）
     * <p>对应数据库 agent 表的 execution_strategy 字段 (varchar)。
     * 若此字段为空或不存在，默认使用 PLAN_EXECUTE 策略</p>
     */
    @Column(name = "execution_strategy", length = 20)
    private String executionStrategy;
}