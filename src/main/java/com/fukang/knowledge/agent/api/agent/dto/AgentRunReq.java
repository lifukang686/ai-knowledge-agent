package com.fukang.knowledge.agent.api.agent.dto;

/**
 * Agent 执行任务请求
 *
 * @param task 用户任务描述（自然语言）
 */
public record AgentRunReq(String task) {}