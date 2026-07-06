package com.fukang.knowledge.agent.module.knowledge.model.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新知识库命令，空字段表示保持原值。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateKnowledgeBaseCommand {
    private String name;

    private String description;

}
