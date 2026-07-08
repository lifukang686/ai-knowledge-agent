package com.fukang.knowledge.agent.module.knowledge.service.storage;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fukang.knowledge.agent.module.knowledge.model.vo.ChunkResult;
import com.fukang.knowledge.agent.module.knowledge.model.vo.ParsedChunk;
import com.fukang.knowledge.agent.module.knowledge.model.vo.ChunkStorageResult;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.module.rag.service.ChineseTextTokenizer;
import com.fukang.knowledge.agent.module.knowledge.model.entity.DocumentChunkEntity;
import com.fukang.knowledge.agent.module.knowledge.mapper.DocumentChunkMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 文档块存储服务。
 * <p>只封装 document_chunk 表的批量写入、查询、删除和实体转换，不承载文档处理流程编排。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentChunkStorageService extends ServiceImpl<DocumentChunkMapper, DocumentChunkEntity> {

    private final ChineseTextTokenizer chineseTextTokenizer;

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
    public ChunkStorageResult saveBatch(List<DocumentChunkEntity> chunks, Long documentId) {
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
    public List<DocumentChunkEntity> findByDocumentId(Long documentId) {
        return baseMapper.selectList(
                new LambdaQueryWrapper<DocumentChunkEntity>()
                        .eq(DocumentChunkEntity::getDocumentId, documentId)
                        .orderByAsc(DocumentChunkEntity::getChunkOrder)
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
                new LambdaQueryWrapper<DocumentChunkEntity>()
                        .eq(DocumentChunkEntity::getDocumentId, documentId)
        );
        log.info("已删除文档块: documentId={}, count={}", documentId, deleted);
        return deleted;
    }

    /**
     * 从 ChunkResult 转换为 DocumentChunkEntity 列表
     * <p>将分块结果中的每个 DocumentChunkEntity 转换为可持久化的 DO 对象</p>
     *
     * @param chunkResult 文档分块结果
     * @param documentId  关联的文档ID
     * @return 待持久化的 DO 列表
     */
    public List<DocumentChunkEntity> toChunkDOList(ChunkResult chunkResult, Long documentId) {
        List<DocumentChunkEntity> chunkDOs = new ArrayList<>();
        for (ParsedChunk chunk : chunkResult.getChunks()) {
            DocumentChunkEntity chunkDO = new DocumentChunkEntity();
            chunkDO.setDocumentId(documentId);
            chunkDO.setChunkText(chunk.getChunkText());
            chunkDO.setSearchText(chineseTextTokenizer.tokenize(chunk.getChunkText()));
            chunkDO.setPageNumber(parseInteger(chunk.getMetadata().get("pageNumber")));
            chunkDO.setSectionTitle(chunk.getMetadata().get("sectionTitle"));
            chunkDO.setChunkOrder(chunk.getChunkOrder());
            chunkDO.setTokenCount(chunk.getTokenCount());
            chunkDOs.add(chunkDO);
        }
        return chunkDOs;
    }

    private Integer parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.debug("解析文档块数字元数据失败: value={}", value);
            return null;
        }
    }
}
