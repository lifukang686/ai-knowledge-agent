package com.fukang.knowledge.agent.module.knowledge.model.resp;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档上传响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUploadResp {
    /** 新创建的文档ID */
    private Long documentId;

    /** 文档入库处理状态 */
    private String status;

}
