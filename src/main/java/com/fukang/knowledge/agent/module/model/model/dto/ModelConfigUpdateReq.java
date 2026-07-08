package com.fukang.knowledge.agent.module.model.model.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模型配置更新请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelConfigUpdateReq {
    /** 所属提供商ID，可选 */
    private Long providerId;

    /** 模型名称（如 gpt-3.5-turbo），可选 */
    private String modelName;

    /** 模型类型（CHAT/EMBEDDING/RERANK/STT），可选 */
    private String modelType;

    /** 默认调用参数，JSON 格式字符串，可选 */
    private String defaultParams;

}