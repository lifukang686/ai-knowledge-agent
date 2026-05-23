package com.fukang.knowledge.agent.application.knowledge;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fukang.knowledge.agent.application.knowledge.model.ChunkStorageResult;
import com.fukang.knowledge.agent.application.knowledge.model.ChunkStorageResult.FailedChunkDetail;
import com.fukang.knowledge.agent.application.knowledge.model.EmbeddingResult;
import com.fukang.knowledge.agent.application.knowledge.model.EmbeddingResult.EmbeddingVector;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.infrastructure.ai.Langchain4jEmbeddingStoreFactory;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.DocumentChunkDO;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.EmbeddingIndexDO;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.EmbeddingIndexMapper;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 嵌入向量索引批量存储服务
 * <p>负责向量数据的批量持久化操作，提供事务管理、数据校验和结果反馈。
 * 继承 MyBatis-Plus ServiceImpl 以获得批量插入等能力。
 * 现已升级为 pgvector 原生向量存储 + langchain4j PgVectorEmbeddingStore，
 * 同时保留 MyBatis-Plus 兼容层用于查询和删除操作</p>
 *
 * <p>存储模式：
 * <ul>
 *   <li>{@link #saveAllInTransaction} - 全事务模式，任意失败整体回滚</li>
 *   <li>{@link #saveAllWithPartialFailure} - 逐条模式，收集失败详情继续执行</li>
 *   <li>{@link #saveBatch} - 批量插入模式，利用 MyBatis-Plus 高效批量写入</li>
 *   <li>{@link #saveVectors} - pgvector 原生批量存储，通过 langchain4j 写入</li>
 *   <li>{@link #saveVector} - pgvector 原生单条存储，同时写入 MyBatis-Plus 兼容记录</li>
 * </ul>
 */
@Slf4j
@Service
public class EmbeddingIndexStorageService extends ServiceImpl<EmbeddingIndexMapper, EmbeddingIndexDO> {

    private final Langchain4jEmbeddingStoreFactory storeFactory;

    public EmbeddingIndexStorageService(Langchain4jEmbeddingStoreFactory storeFactory) {
        this.storeFactory = storeFactory;
    }

    /**
     * 事务批量保存嵌入向量索引（全量成功或整体回滚）
     * <p>在一个事务中循环插入所有向量索引记录。任意一条插入失败时抛出异常，
     * 触发事务回滚。适用于对数据一致性要求高的场景</p>
     *
     * @param indexDOs  待存储的嵌入向量索引 DO 列表
     * @param chunkIds  关联的块 ID 列表
     * @return 全部成功的存储结果
     * @throws BaseException 任意向量索引存储失败时抛出 VECTOR_STORAGE_FAILED
     */
    @Transactional(rollbackFor = Exception.class)
    public ChunkStorageResult saveAllInTransaction(List<EmbeddingIndexDO> indexDOs, List<Long> chunkIds) {
        if (indexDOs == null || indexDOs.isEmpty()) {
            log.warn("待存储的向量索引列表为空");
            throw new BaseException(ErrorCodeEnum.CHUNK_DATA_EMPTY);
        }

        log.info("开始事务批量存储向量索引: count={}", indexDOs.size());

        for (EmbeddingIndexDO indexDO : indexDOs) {
            int inserted = baseMapper.insert(indexDO);
            if (inserted != 1) {
                log.error("向量索引存储失败: chunkId={}", indexDO.getChunkId());
                throw new BaseException(ErrorCodeEnum.VECTOR_STORAGE_FAILED);
            }
        }

        log.info("事务批量存储向量索引完成: count={}", indexDOs.size());
        return ChunkStorageResult.allSuccess(0L, indexDOs.size());
    }

    /**
     * 逐条保存向量索引（允许部分失败）
     * <p>逐条插入每个向量索引记录，单条失败不中断后续存储，收集所有失败信息后返回</p>
     *
     * @param indexDOs 待存储的嵌入向量索引 DO 列表
     * @return 包含成功/失败计数的存储结果
     */
    public ChunkStorageResult saveAllWithPartialFailure(List<EmbeddingIndexDO> indexDOs) {
        if (indexDOs == null || indexDOs.isEmpty()) {
            log.warn("待存储的向量索引列表为空");
            throw new BaseException(ErrorCodeEnum.CHUNK_DATA_EMPTY);
        }

        log.info("开始逐条存储向量索引: count={}", indexDOs.size());

        int successCount = 0;
        List<FailedChunkDetail> failedDetails = new ArrayList<>();

        for (EmbeddingIndexDO indexDO : indexDOs) {
            try {
                int inserted = baseMapper.insert(indexDO);
                if (inserted == 1) {
                    successCount++;
                } else {
                    failedDetails.add(new FailedChunkDetail(
                            0, "向量索引 insert 返回结果异常: " + inserted));
                }
            } catch (Exception e) {
                log.error("向量索引存储异常: chunkId={}", indexDO.getChunkId(), e);
                failedDetails.add(new FailedChunkDetail(
                        0, e.getMessage() != null ? e.getMessage() : "未知错误"));
            }
        }

        log.info("逐条存储向量索引完成: total={}, success={}, failed={}",
                indexDOs.size(), successCount, failedDetails.size());

        return ChunkStorageResult.withFailures(0L, indexDOs.size(), successCount, failedDetails);
    }

    /**
     * 批量插入向量索引（MyBatis-Plus saveBatch）
     * <p>使用 MyBatis-Plus ServiceImpl 的 saveBatch 方法进行高效批量插入，
     * 默认每批次 50 条。在事务中执行，失败时整体回滚</p>
     *
     * @param indexDOs 待存储的嵌入向量索引 DO 列表
     * @return 全部成功的存储结果
     * @throws BaseException 批量存储失败时抛出 VECTOR_STORAGE_FAILED
     */
    @Transactional(rollbackFor = Exception.class)
    public ChunkStorageResult saveBatch(List<EmbeddingIndexDO> indexDOs) {
        if (indexDOs == null || indexDOs.isEmpty()) {
            log.warn("待存储的向量索引列表为空");
            throw new BaseException(ErrorCodeEnum.CHUNK_DATA_EMPTY);
        }

        log.info("开始 MyBatis-Plus 批量插入向量索引: count={}", indexDOs.size());

        try {
            super.saveBatch(indexDOs, 50);
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("批量插入向量索引异常", e);
            throw new BaseException(ErrorCodeEnum.VECTOR_STORAGE_FAILED);
        }

        log.info("批量插入向量索引完成: count={}", indexDOs.size());

        return ChunkStorageResult.allSuccess(0L, indexDOs.size());
    }

    /**
     * 通过 PgVectorEmbeddingStore 批量存储向量
     * <p>遍历 TextSegment 和 Embedding 列表，逐一写入 pgvector 存储</p>
     *
     * @param segments   文本段列表，包含块文本和元数据
     * @param embeddings langchain4j 嵌入向量列表，与 segments 一一对应
     */
    public void saveVectors(List<TextSegment> segments, List<Embedding> embeddings) {
        if (segments == null || segments.isEmpty()) {
            log.warn("待存储的向量列表为空");
            return;
        }
        if (segments.size() != embeddings.size()) {
            log.error("segment 与 embedding 数量不一致: segments={}, embeddings={}",
                    segments.size(), embeddings.size());
            throw new BaseException(ErrorCodeEnum.CHUNK_DATA_EMPTY);
        }

        log.info("开始通过 PgVectorEmbeddingStore 批量存储向量: count={}", segments.size());
        PgVectorEmbeddingStore store = storeFactory.createEmbeddingStore();

        List<String> ids = new ArrayList<>(segments.size());
        for (int i = 0; i < segments.size(); i++) {
            ids.add(UUID.randomUUID().toString());
        }
        store.addAll(embeddings, segments);
        log.info("PgVectorEmbeddingStore 批量存储完成: count={}", segments.size());
    }

    /**
     * 通过 PgVectorEmbeddingStore 存储单条向量，同时写入 MyBatis-Plus 兼容记录
     * <p>将向量写入 pgvector 原生存储，并同步在 embedding_index 表中写入元数据记录，
     * 确保已有的 MyBatis-Plus 查询/删除方法仍可正常工作</p>
     *
     * @param id              向量记录唯一标识
     * @param vector          嵌入向量数组
     * @param chunkId         关联的文档块 ID
     * @param knowledgeBaseId 所属知识库 ID
     */
    public void saveVector(String id, float[] vector, String chunkId, Long knowledgeBaseId) {
        log.info("通过 PgVectorEmbeddingStore 存储单条向量: id={}, chunkId={}, kbId={}", id, chunkId, knowledgeBaseId);

        PgVectorEmbeddingStore store = storeFactory.createEmbeddingStore();

        Embedding embedding = Embedding.from(vector);

        dev.langchain4j.data.document.Metadata metadata =
                new dev.langchain4j.data.document.Metadata();
        metadata.put("chunk_id", chunkId);
        metadata.put("knowledge_base_id", String.valueOf(knowledgeBaseId));

        TextSegment segment = TextSegment.from("", metadata);

        store.add(embedding, segment);

        EmbeddingIndexDO indexDO = new EmbeddingIndexDO();
        indexDO.setId(Long.valueOf(id));
        indexDO.setChunkId(Long.valueOf(chunkId));
        indexDO.setKnowledgeBaseId(knowledgeBaseId);
        indexDO.setVector(vectorToJson(vector));
        indexDO.setMetadata(buildMetadataForSingle(chunkId, knowledgeBaseId));
        baseMapper.insert(indexDO);

        log.info("单条向量写入完成: id={}, chunkId={}", id, chunkId);
    }

    /**
     * 将向量化结果和文档块列表转换为嵌入索引 DO 列表
     * <p>将每个块的向量数据序列化为 JSON 字符串，创建 EmbeddingIndexDO 对象</p>
     *
     * @param embeddingResult 向量嵌入结果
     * @param chunks          对应的文档块列表（需包含已持久化的 ID）
     * @param knowledgeBaseId 所属知识库 ID
     * @return 待持久化的嵌入向量索引 DO 列表
     */
    public List<EmbeddingIndexDO> toIndexDOList(EmbeddingResult embeddingResult,
                                                 List<DocumentChunkDO> chunks,
                                                 Long knowledgeBaseId) {
        List<EmbeddingIndexDO> indexDOs = new ArrayList<>();

        for (EmbeddingVector embeddingVector : embeddingResult.embeddings()) {
            int chunkOrder = embeddingVector.chunkOrder();
            if (chunkOrder >= chunks.size()) {
                log.warn("向量顺序号超出块列表范围: chunkOrder={}, chunkListSize={}",
                        chunkOrder, chunks.size());
                continue;
            }

            DocumentChunkDO chunk = chunks.get(chunkOrder);
            EmbeddingIndexDO indexDO = new EmbeddingIndexDO();
            indexDO.setChunkId(chunk.getId());
            indexDO.setKnowledgeBaseId(knowledgeBaseId);
            indexDO.setVector(vectorToJson(embeddingVector.vector()));

            String metadata = buildMetadata(embeddingResult, embeddingVector);
            indexDO.setMetadata(metadata);

            indexDOs.add(indexDO);
        }

        return indexDOs;
    }

    /**
     * 根据块ID查询向量索引
     *
     * @param chunkId 文档块ID
     * @return 向量索引列表
     */
    public List<EmbeddingIndexDO> findByChunkId(Long chunkId) {
        return baseMapper.selectList(
                new LambdaQueryWrapper<EmbeddingIndexDO>()
                        .eq(EmbeddingIndexDO::getChunkId, chunkId)
        );
    }

    /**
     * 根据知识库ID查询所有向量索引
     *
     * @param knowledgeBaseId 知识库ID
     * @return 该知识库下所有向量索引
     */
    public List<EmbeddingIndexDO> findByKnowledgeBaseId(Long knowledgeBaseId) {
        return baseMapper.selectList(
                new LambdaQueryWrapper<EmbeddingIndexDO>()
                        .eq(EmbeddingIndexDO::getKnowledgeBaseId, knowledgeBaseId)
        );
    }

    /**
     * 根据知识库ID删除所有向量索引
     *
     * @param knowledgeBaseId 知识库ID
     * @return 删除的记录数量
     */
    public int deleteByKnowledgeBaseId(Long knowledgeBaseId) {
        int deleted = baseMapper.delete(
                new LambdaQueryWrapper<EmbeddingIndexDO>()
                        .eq(EmbeddingIndexDO::getKnowledgeBaseId, knowledgeBaseId)
        );
        log.info("已删除向量索引: knowledgeBaseId={}, count={}", knowledgeBaseId, deleted);
        return deleted;
    }

    /**
     * 根据块ID列表删除向量索引
     *
     * @param chunkIds 块ID列表
     * @return 删除的记录数量
     */
    public int deleteByChunkIds(List<Long> chunkIds) {
        if (chunkIds == null || chunkIds.isEmpty()) {
            return 0;
        }
        int deleted = baseMapper.delete(
                new LambdaQueryWrapper<EmbeddingIndexDO>()
                        .in(EmbeddingIndexDO::getChunkId, chunkIds)
        );
        log.info("已删除向量索引: chunkCount={}, deletedCount={}", chunkIds.size(), deleted);
        return deleted;
    }

    /**
     * 将 float 数组序列化为 JSON 格式的向量字符串
     * <p>格式: [0.123, 0.456, ...]</p>
     *
     * @param vector float 数组
     * @return JSON 格式的向量字符串
     */
    private String vectorToJson(float[] vector) {
        if (vector == null || vector.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * 构建向量元数据 JSON 字符串
     *
     * @param embeddingResult 向量嵌入结果
     * @param embeddingVector 单个向量嵌入数据
     * @return 元数据 JSON 字符串
     */
    private String buildMetadata(EmbeddingResult embeddingResult, EmbeddingVector embeddingVector) {
        return String.format(
                "{\"modelName\":\"%s\",\"dimension\":%d,\"chunkOrder\":%d,\"totalTokens\":%d}",
                embeddingResult.modelName(),
                embeddingVector.dimension(),
                embeddingVector.chunkOrder(),
                embeddingResult.totalTokens()
        );
    }

    private String buildMetadataForSingle(String chunkId, Long knowledgeBaseId) {
        return String.format(
                "{\"chunkId\":\"%s\",\"knowledgeBaseId\":%d,\"storeType\":\"pgvector\"}",
                chunkId, knowledgeBaseId
        );
    }
}