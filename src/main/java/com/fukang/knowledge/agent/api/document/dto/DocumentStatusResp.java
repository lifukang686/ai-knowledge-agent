package com.fukang.knowledge.agent.api.document.dto;

/**
 * 文档状态查询响应 DTO
 *
 * @param status 文档当前处理状态（如 pending、processing、completed、failed）
 */
public record DocumentStatusResp(String status) {}