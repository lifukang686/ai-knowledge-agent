package com.fukang.knowledge.agent.module.knowledge.model.resp;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档状态查询响应 DTO
 *
 * @param status 文档当前处理状态（如 pending、processing、completed、failed）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentStatusResp {
    private String status;

}