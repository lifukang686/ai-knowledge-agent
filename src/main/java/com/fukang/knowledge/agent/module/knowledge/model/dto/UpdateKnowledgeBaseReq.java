package com.fukang.knowledge.agent.module.knowledge.model.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新知识库请求 DTO
 * <p>仅更新非空字段，未传字段保持原值不变</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateKnowledgeBaseReq {
    /** 知识库名称，可选 */
    private String name;

    /** 知识库描述，可选 */
    private String description;

}
