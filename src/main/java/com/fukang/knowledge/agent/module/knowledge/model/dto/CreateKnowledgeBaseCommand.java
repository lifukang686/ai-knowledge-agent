package com.fukang.knowledge.agent.module.knowledge.model.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建知识库命令。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateKnowledgeBaseCommand {
    private String name;

    private String description;

}
