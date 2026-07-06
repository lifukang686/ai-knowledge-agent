package com.fukang.knowledge.agent.module.knowledge.model.resp;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 知识库响应 DTO
 * <p>返回给前端的知识库完整信息，包含基础信息和统计字段</p>
 *
 * @param id            知识库ID（Long→String 序列化）
 * @param name          知识库名称
 * @param description   知识库描述
 * @param documentCount 文档数量
 * @param status        知识库状态
 * @param createTime    创建时间（格式化: yyyy-MM-dd HH:mm:ss）
 * @param updateTime    更新时间（格式化: yyyy-MM-dd HH:mm:ss）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeBaseResp {
    private Long id;

    private String name;

    private String description;

    private long documentCount;

    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

}
