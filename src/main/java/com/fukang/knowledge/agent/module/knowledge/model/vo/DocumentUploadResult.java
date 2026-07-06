package com.fukang.knowledge.agent.module.knowledge.model.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档上传后的应用层返回结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUploadResult {
    private Long documentId;

    private String status;

}
