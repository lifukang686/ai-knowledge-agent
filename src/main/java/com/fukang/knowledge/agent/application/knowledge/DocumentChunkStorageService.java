package com.fukang.knowledge.agent.application.knowledge;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fukang.knowledge.agent.application.knowledge.model.ChunkResult;
import com.fukang.knowledge.agent.application.knowledge.model.ChunkResult.DocumentChunk;
import com.fukang.knowledge.agent.application.knowledge.model.ChunkStorageResult;
import com.fukang.knowledge.agent.application.knowledge.model.ChunkStorageResult.FailedChunkDetail;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.DocumentChunkDO;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.DocumentChunkMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 文档块批量存储服务
 * <p>负责文档块的批量持久化操作，提供事务管理、数据校验和结果反馈。
 * 继承 MyBatis-Plus ServiceImpl 以获得批量插入等能力。
 * 设计为基础设施层的存储服务，仅处理数据库写入逻辑，不包含业务编排</p>
 *
 * <p>提供三种存储模式：
 * <ul>
 *   <li>{@link #saveAllInTransaction} - 全事务模式，任意失败整体回滚</li>
 *   <li>{@link #saveAllWithPartialFailure} - 逐条模式，收集失败详情继续执行</li>
 *   <li>{@link #saveBatch} - 批量插入模式，利用 MyBatis-Plus 高效批量写入</li>
 * </ul>
 */
@Slf4j
@Service
public class DocumentChunkStorageService extends ServiceImpl<DocumentChunkMapper, DocumentChunkDO> {

    /**
     * 事务批量保存文档块（全量成功或整体回滚）
     * <p>在一个事务中循环插入所有块。任意一条插入返回非 1 时抛出异常，
     * 触发事务回滚。适用于对数据一致性要求高的场景</p>
     *
     * @param chunks     待存储的文档块 DO 列表
     * @param documentId 关联的文档ID
     * @return 全部成功的存储结果
     * @throws BaseException 任意块存储失败时抛出 CHUNK_STORAGE_FAILED
     */
    @Transactional(rollbackFor = Exception.class)
    public ChunkStorageResult saveAllInTransaction(List<DocumentChunkDO> chunks, Long documentId) {
        if (chunks == null || chunks.isEmpty()) {
            log.warn("待存储的文档块列表为空: documentId={}", documentId);
            throw new BaseException(ErrorCodeEnum.CHUNK_DATA_EMPTY);
        }

        log.info("开始事务批量存储文档块: documentId={}, count={}", documentId, chunks.size());

        for (DocumentChunkDO chunk : chunks) {
            int inserted = baseMapper.insert(chunk);
            if (inserted != 1) {
                log.error("文档块存储失败: documentId={}, chunkOrder={}", documentId, chunk.getChunkOrder());
                throw new BaseException(ErrorCodeEnum.CHUNK_STORAGE_FAILED);
            }
        }

        log.info("事务批量存储完成: documentId={}, count={}", documentId, chunks.size());
        return ChunkStorageResult.allSuccess(documentId, chunks.size());
    }

    /**
     * 逐条保存文档块（允许部分失败）
     * <p>逐条插入每个块，单条失败不中断后续存储，收集所有失败信息后返回。
     * 适用于对可用性要求高于一致性的场景</p>
     *
     * @param chunks     待存储的文档块 DO 列表
     * @param documentId 关联的文档ID
     * @return 包含成功/失败计数的存储结果
     */
    public ChunkStorageResult saveAllWithPartialFailure(List<DocumentChunkDO> chunks, Long documentId) {
        if (chunks == null || chunks.isEmpty()) {
            log.warn("待存储的文档块列表为空: documentId={}", documentId);
            throw new BaseException(ErrorCodeEnum.CHUNK_DATA_EMPTY);
        }

        log.info("开始逐条存储文档块: documentId={}, count={}", documentId, chunks.size());

        int successCount = 0;
        List<FailedChunkDetail> failedDetails = new ArrayList<>();

        for (DocumentChunkDO chunk : chunks) {
            try {
                int inserted = baseMapper.insert(chunk);
                if (inserted == 1) {
                    successCount++;
                } else {
                    failedDetails.add(new FailedChunkDetail(
                            chunk.getChunkOrder(), "insert 返回结果异常: " + inserted));
                }
            } catch (Exception e) {
                log.error("文档块存储异常: documentId={}, chunkOrder={}",
                        documentId, chunk.getChunkOrder(), e);
                failedDetails.add(new FailedChunkDetail(
                        chunk.getChunkOrder(),
                        e.getMessage() != null ? e.getMessage() : "未知错误"));
            }
        }

        log.info("逐条存储完成: documentId={}, total={}, success={}, failed={}",
                documentId, chunks.size(), successCount, failedDetails.size());

        return ChunkStorageResult.withFailures(documentId, chunks.size(), successCount, failedDetails);
    }

    /**
     * 批量插入文档块（MyBatis-Plus saveBatch）
     * <p>使用 MyBatis-Plus ServiceImpl 的 saveBatch 方法进行高效批量插入，
     * 默认每批次 50 条，JDBC 层合并为批量 SQL 执行。
     * 在事务中执行，失败时整体回滚。
     * 需确保 JDBC URL 已配置 rewriteBatchedStatements=true</p>
     *
     * @param chunks     待存储的文档块 DO 列表
     * @param documentId 关联的文档ID
     * @return 全部成功的存储结果
     * @throws BaseException 批量存储失败时抛出 CHUNK_STORAGE_FAILED
     */
    @Transactional(rollbackFor = Exception.class)
    public ChunkStorageResult saveBatch(List<DocumentChunkDO> chunks, Long documentId) {
        if (chunks == null || chunks.isEmpty()) {
            log.warn("待存储的文档块列表为空: documentId={}", documentId);
            throw new BaseException(ErrorCodeEnum.CHUNK_DATA_EMPTY);
        }

        log.info("开始 MyBatis-Plus 批量插入文档块: documentId={}, count={}", documentId, chunks.size());

        try {
            super.saveBatch(chunks, 50);
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("批量插入异常: documentId={}", documentId, e);
            throw new BaseException(ErrorCodeEnum.CHUNK_STORAGE_FAILED);
        }

        log.info("批量插入完成: documentId={}, count={}", documentId, chunks.size());

        return ChunkStorageResult.allSuccess(documentId, chunks.size());
    }

    /**
     * 根据文档ID查询所有块（按 chunkOrder 升序）
     *
     * @param documentId 文档ID
     * @return 该文档的所有块列表
     */
    public List<DocumentChunkDO> findByDocumentId(Long documentId) {
        return baseMapper.selectList(
                new LambdaQueryWrapper<DocumentChunkDO>()
                        .eq(DocumentChunkDO::getDocumentId, documentId)
                        .orderByAsc(DocumentChunkDO::getChunkOrder)
        );
    }

    /**
     * 根据文档ID删除所有块
     *
     * @param documentId 文档ID
     * @return 删除的块数量
     */
    public int deleteByDocumentId(Long documentId) {
        int deleted = baseMapper.delete(
                new LambdaQueryWrapper<DocumentChunkDO>()
                        .eq(DocumentChunkDO::getDocumentId, documentId)
        );
        log.info("已删除文档块: documentId={}, count={}", documentId, deleted);
        return deleted;
    }

    /**
     * 统计文档的块数量
     *
     * @param documentId 文档ID
     * @return 块数量
     */
    public long countByDocumentId(Long documentId) {
        return baseMapper.selectCount(
                new LambdaQueryWrapper<DocumentChunkDO>()
                        .eq(DocumentChunkDO::getDocumentId, documentId)
        );
    }

    /**
     * 从 ChunkResult 转换为 DocumentChunkDO 列表
     * <p>将分块结果中的每个 DocumentChunk 转换为可持久化的 DO 对象</p>
     *
     * @param chunkResult 文档分块结果
     * @param documentId  关联的文档ID
     * @return 待持久化的 DO 列表
     */
    public List<DocumentChunkDO> toChunkDOList(ChunkResult chunkResult, Long documentId) {
        List<DocumentChunkDO> chunkDOs = new ArrayList<>();
        for (DocumentChunk chunk : chunkResult.chunks()) {
            DocumentChunkDO chunkDO = new DocumentChunkDO();
            chunkDO.setDocumentId(documentId);
            chunkDO.setChunkText(chunk.chunkText());
            chunkDO.setChunkOrder(chunk.chunkOrder());
            chunkDO.setTokenCount(chunk.tokenCount());
            chunkDOs.add(chunkDO);
        }
        return chunkDOs;
    }
}