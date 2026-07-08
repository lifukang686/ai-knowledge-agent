package com.fukang.knowledge.agent.module.knowledge.model.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 文档块存储结果。
 * <p>当前管道采用整体事务写入，结果只记录总数、成功数和完成时间。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChunkStorageResult {
    /** 关联的文档ID */
    private Long documentId;

    /** 待存储的块总数 */
    private int totalCount;

    /** 成功存储的块数量 */
    private int successCount;

    /** 存储完成时间 */
    private LocalDateTime storageTime;

    /**
     * 创建全部成功的存储结果
     *
     * @param documentId 文档ID
     * @param totalCount 块总数
     * @return 全部成功的存储结果
     */
    public static ChunkStorageResult allSuccess(Long documentId, int totalCount) {
        return new ChunkStorageResult(
                documentId, totalCount, totalCount, LocalDateTime.now()
        );
    }
}
