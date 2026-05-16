package com.fukang.knowledge.agent.application.knowledge.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 文档块存储结果
 * <p>封装批量存储文档块的执行结果反馈，包含成功/失败计数和失败详情，
 * 供调用方判断存储是否全部成功或需要处理部分失败情况</p>
 *
 * @param documentId     关联的文档ID
 * @param totalCount     待存储的块总数
 * @param successCount   成功存储的块数量
 * @param failedCount    存储失败的块数量
 * @param failedDetails  失败详情列表，每个条目包含块序号和失败原因
 * @param storageTime    存储完成时间
 * @param allSucceeded   是否全部存储成功
 */
public record ChunkStorageResult(
        Long documentId,
        int totalCount,
        int successCount,
        int failedCount,
        List<FailedChunkDetail> failedDetails,
        LocalDateTime storageTime,
        boolean allSucceeded
) {

    /**
     * 单个失败的块详情
     *
     * @param chunkOrder 失败的块在文档中的顺序号
     * @param reason     失败原因描述
     */
    public record FailedChunkDetail(
            int chunkOrder,
            String reason
    ) {
    }

    /**
     * 创建全部成功的存储结果
     *
     * @param documentId 文档ID
     * @param totalCount 块总数
     * @return 全部成功的存储结果
     */
    public static ChunkStorageResult allSuccess(Long documentId, int totalCount) {
        return new ChunkStorageResult(
                documentId, totalCount, totalCount, 0,
                List.of(), LocalDateTime.now(), true
        );
    }

    /**
     * 创建包含失败信息的存储结果
     *
     * @param documentId    文档ID
     * @param totalCount    块总数
     * @param successCount  成功数
     * @param failedDetails 失败详情列表
     * @return 包含失败详情的存储结果
     */
    public static ChunkStorageResult withFailures(Long documentId, int totalCount,
                                                   int successCount,
                                                   List<FailedChunkDetail> failedDetails) {
        return new ChunkStorageResult(
                documentId, totalCount, successCount,
                failedDetails.size(), List.copyOf(failedDetails),
                LocalDateTime.now(), failedDetails.isEmpty()
        );
    }
}