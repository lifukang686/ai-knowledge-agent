package com.fukang.knowledge.agent.api.agent.dto;

/**
 * Agent 执行任务请求
 *
 * @param task              用户任务描述（自然语言）
 * @param executionStrategy 执行策略，可选 PLAN_EXECUTE（默认）/ AI_SERVICES
 */
public record AgentRunReq(String task, String executionStrategy) {}