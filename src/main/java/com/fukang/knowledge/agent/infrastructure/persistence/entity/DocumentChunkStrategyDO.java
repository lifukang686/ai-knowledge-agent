package com.fukang.knowledge.agent.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文档分块策略配置实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "chunk_strategy")
@TableName("chunk_strategy")
public class DocumentChunkStrategyDO extends BaseEntity {

    /** 分块策略名称 */
    @Column(name = "strategy_name", nullable = false, length = 100)
    private String strategyName;

    /** 分块类型：按内容归属、按语义、按段落、按句子、按字符 */
    @Column(name = "chunk_type", nullable = false, length = 32)
    private String chunkType;

    /** 单个 chunk 最大字符数 */
    @Column(name = "max_segment_size", nullable = false)
    private Integer maxSegmentSize;

    /** 相邻 chunk 重叠字符数 */
    @Column(name = "overlap_size", nullable = false)
    private Integer overlapSize;

    /** 是否默认策略 */
    @Column(name = "is_default")
    private Boolean isDefault = false;
}
