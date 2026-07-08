package com.fukang.knowledge.agent.module.knowledge.model.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;

/**
 * 创建知识库请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateKnowledgeBaseReq {
    /** 知识库名称，不能为空，1-100个字符 */
    @NotBlank(message = "知识库名称不能为空")
    private String name;

    /** 知识库描述，可选，最多500个字符 */
    private String description;

}
