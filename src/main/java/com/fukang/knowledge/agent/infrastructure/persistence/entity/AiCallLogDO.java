package com.fukang.knowledge.agent.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 调用日志实体类
 * <p>对应数据库表 ai_call_log，记录每次 AI 模型调用的详细信息，
 * 用于调用审计、Token 消耗统计和性能分析</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_call_log")
public class AiCallLogDO extends BaseEntity {

    /** 关联的模型配置ID */
    private Long modelId;

    /** 用户输入的提示词 */
    private String prompt;

    /** AI 返回的响应文本 */
    private String response;

    /** 本次调用消耗的 Token 数 */
    private Integer tokenUsage;

    /** 本次调用耗时（毫秒） */
    private Integer latencyMs;
}
