package com.fukang.knowledge.agent.module.knowledge.model.resp;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档上传响应 DTO
 *
 * @param documentId 新创建的文档ID
 * @param status     文档入库处理状态
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUploadResp {
    private Long documentId;

    private String status;

}
