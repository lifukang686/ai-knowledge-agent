package com.fukang.knowledge.agent.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Agent 模块配置属性
 * <p>定义 Agent 执行的核心参数，通过 application.yml 进行类型安全配置。
 * 使用 knowledge-agent.agent 前缀</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "knowledge-agent.agent")
public class AgentProperties {

    /** 最大执行步数限制，防止无限循环，默认 10 */
    private int maxSteps = 10;

    /** 单次工具调用超时时间（毫秒），默认 30 秒 */
    private long toolTimeoutMs = 30000;

    /** 规划阶段 LLM 调用超时时间（毫秒），默认 60 秒 */
    private long planningTimeoutMs = 60000;

    /** 是否启用并行工具调用（二期功能，暂不启用） */
    private boolean parallelExecution = false;
}