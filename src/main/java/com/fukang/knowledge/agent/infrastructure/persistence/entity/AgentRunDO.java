package com.fukang.knowledge.agent.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Agent 运行记录实体类
 * <p>对应数据库表 agent_run，保存每次 Agent 任务执行的完整记录，
 * 包括开始/结束时间、状态、输入输出和步骤日志</p>
 */
@Data
@Entity
@Table(name = "agent_run")
@TableName("agent_run")
public class AgentRunDO {

    /** 主键ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Agent 配置ID，关联 agent 表 */
    @Column(name = "agent_id", nullable = false)
    private Long agentId;

    /** 用户输入的原始任务查询文本 */
    @Column(name = "input_query", columnDefinition = "text")
    private String inputQuery;

    /** 最终生成的回答文本 */
    @Column(name = "output_answer", columnDefinition = "text")
    private String outputAnswer;

    /** 运行状态: PLANNING / EXECUTING / COMPLETED / FAILED / CANCELLED */
    @Column(name = "status", length = 20)
    private String status;

    /** 执行日志（JSON 格式，含每一步的详情） */
    @Column(name = "log", columnDefinition = "text")
    private String log;

    /** 错误信息 */
    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    /** 开始执行时间 */
    @Column(name = "start_time")
    private LocalDateTime startTime;

    /** 结束执行时间 */
    @Column(name = "end_time")
    private LocalDateTime endTime;

    /** 创建时间 */
    @Column(name = "create_time")
    private LocalDateTime createTime;
}