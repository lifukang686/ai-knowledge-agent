package com.fukang.knowledge.agent.module.knowledge.application.result;

/**
 * 文档上传后的应用层返回结果。
 */
public record DocumentUploadResult(
        Long documentId,
        String status
) {}
