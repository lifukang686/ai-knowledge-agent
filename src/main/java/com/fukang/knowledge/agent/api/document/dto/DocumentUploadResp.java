package com.fukang.knowledge.agent.api.document.dto;

/**
 * 文档上传响应 DTO
 *
 * @param documentId 新创建的文档ID
 * @param status     文档入库处理状态
 */
public record DocumentUploadResp(Long documentId, String status) {}
