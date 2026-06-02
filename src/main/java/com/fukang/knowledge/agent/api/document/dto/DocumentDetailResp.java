package com.fukang.knowledge.agent.api.document.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 文档详情响应 DTO
 * <p>返回给前端的文档完整信息，包含元数据和解析后的文本内容。
 * 用于文档详情浏览页面展示文档标题、内容、创建时间、更新时间等完整信息。</p>
 *
 * @param id              文档ID（Long→String 序列化）
 * @param title           文档标题（原始文件名）
 * @param content         文档解析文本，由已入库的 chunk 拼接得到
 * @param filePath        文件在 MinIO 中的存储路径
 * @param knowledgeBaseId 所属知识库ID
 * @param status          处理状态
 * @param uploadedBy      上传者
 * @param chunkCount      分块数量
 * @param fileSize        文件大小（字节）
 * @param embeddingModelId 文档向量化实际使用的模型配置 ID
 * @param embeddingDimension 文档向量维度
 * @param embeddingVersion 文档向量化模型版本标识
 * @param createTime      创建时间
 * @param updateTime      更新时间
 */
public record DocumentDetailResp(
        Long id,
        String title,
        String content,
        String filePath,
        Long knowledgeBaseId,
        String status,
        String uploadedBy,
        long chunkCount,
        long fileSize,
        Long embeddingModelId,
        Integer embeddingDimension,
        String embeddingVersion,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        LocalDateTime createTime,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        LocalDateTime updateTime
) {}
