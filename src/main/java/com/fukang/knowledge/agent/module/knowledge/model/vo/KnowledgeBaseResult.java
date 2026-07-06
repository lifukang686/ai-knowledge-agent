package com.fukang.knowledge.agent.module.knowledge.model.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 知识库查询结果，包含文档数量统计。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeBaseResult {
    private Long id;

    private String name;

    private String description;

    private long documentCount;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

}
