package com.fukang.knowledge.agent.module.knowledge.model.resp;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 知识库响应 DTO
 * <p>返回给前端的知识库完整信息，包含基础信息和统计字段</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeBaseResp {
    /** 知识库ID（Long→String 序列化） */
    private Long id;

    /** 知识库名称 */
    private String name;

    /** 知识库描述 */
    private String description;

    /** 文档数量 */
    private long documentCount;

    /** 知识库状态 */
    private String status;

    /** 创建时间（格式化: yyyy-MM-dd HH:mm:ss） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /** 更新时间（格式化: yyyy-MM-dd HH:mm:ss） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

}
