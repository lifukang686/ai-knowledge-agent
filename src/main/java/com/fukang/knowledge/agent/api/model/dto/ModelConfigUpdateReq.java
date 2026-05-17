package com.fukang.knowledge.agent.api.model.dto;

/**
 * 模型配置更新请求 DTO
 *
 * @param providerId    所属提供商ID，可选
 * @param modelName     模型名称（如 gpt-3.5-turbo），可选
 * @param modelType     模型类型（CHAT/EMBEDDING/RERANK/STT），可选
 * @param defaultParams 默认调用参数，JSON 格式字符串，可选
 */
public record ModelConfigUpdateReq(

        Long providerId,

        String modelName,

        String modelType,

        String defaultParams
) {}