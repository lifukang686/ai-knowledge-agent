package com.fukang.knowledge.agent.module.knowledge.model.resp;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 文档响应 DTO
 * <p>返回给前端的文档完整信息，匹配前端 KnowledgeBaseEntity.DocumentEntity 类型定义。
 * 预留 preview/download 相关字段扩展（如 preview_url、download_url），后续迭代可直接在此添加</p>
 *
 * @param id              文档ID（Long→String 序列化）
 * @param name            文档名称（原始文件名）
 * @param filePath        文件在 MinIO 中的存储路径
 * @param knowledgeBaseId 所属知识库ID
 * @param status          处理状态（uploaded/pending）
 * @param uploadedBy      上传者（当前为上传者ID，后续可替换为用户名）
 * @param chunkCount      分块数量（当前 MVP 默认为 0，后续解析后更新）
 * @param fileSize        文件大小（字节，当前 MVP 默认为 0）
 * @param createTime      创建时间
 * @param updateTime      更新时间
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResp {
    private Long id;

    private String name;

    private String filePath;

    private Long knowledgeBaseId;

    private String status;

    private String uploadedBy;

    private long chunkCount;

    private long fileSize;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

}
