package com.fukang.knowledge.agent.application.knowledge.chunk;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fukang.knowledge.agent.application.knowledge.command.CreateChunkStrategyCommand;
import com.fukang.knowledge.agent.application.knowledge.command.UpdateChunkStrategyCommand;
import com.fukang.knowledge.agent.application.knowledge.port.DocumentChunkStrategyRepository;
import com.fukang.knowledge.agent.common.enums.ChunkTypeEnum;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.common.result.PageResponse;
import com.fukang.knowledge.agent.infrastructure.chunk.CharacterChunkStrategy;
import com.fukang.knowledge.agent.infrastructure.chunk.ChunkStrategy;
import com.fukang.knowledge.agent.infrastructure.chunk.ParagraphChunkStrategy;
import com.fukang.knowledge.agent.infrastructure.chunk.SentenceChunkStrategy;
import com.fukang.knowledge.agent.infrastructure.config.ChunkingProperties;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.DocumentChunkStrategyDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 文档分块策略应用服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentChunkStrategyAppService {

    private final DocumentChunkStrategyRepository chunkStrategyRepository;
    private final ChunkingProperties chunkingProperties;

    @Transactional(rollbackFor = Exception.class)
    public Long createStrategy(CreateChunkStrategyCommand command) {
        validateChunkType(command.chunkType());
        validateSegmentSize(command.maxSegmentSize(), command.overlapSize());

        DocumentChunkStrategyDO strategy = new DocumentChunkStrategyDO();
        strategy.setStrategyName(command.strategyName());
        strategy.setChunkType(command.chunkType());
        strategy.setMaxSegmentSize(command.maxSegmentSize());
        strategy.setOverlapSize(command.overlapSize());
        strategy.setIsDefault(false);
        chunkStrategyRepository.insert(strategy);
        log.info("分块策略创建成功: id={}, name={}, type={}",
                strategy.getId(), strategy.getStrategyName(), strategy.getChunkType());
        return strategy.getId();
    }

    public PageResponse<DocumentChunkStrategyDO> listStrategies(long page, long pageSize, String keyword) {
        IPage<DocumentChunkStrategyDO> resultPage = chunkStrategyRepository.page(page, pageSize, keyword);
        return new PageResponse<>(
                resultPage.getRecords(),
                resultPage.getTotal(),
                resultPage.getCurrent(),
                resultPage.getSize());
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStrategy(Long id, UpdateChunkStrategyCommand command) {
        DocumentChunkStrategyDO strategy = findStrategyById(id);
        ChunkTypeEnum chunkType = validateChunkType(strategy.getChunkType());
        if (StringUtils.hasText(command.strategyName())) {
            strategy.setStrategyName(command.strategyName());
        }
        if (StringUtils.hasText(command.chunkType())) {
            chunkType = validateChunkType(command.chunkType());
            strategy.setChunkType(command.chunkType());
        }
        if (command.maxSegmentSize() != null) {
            strategy.setMaxSegmentSize(command.maxSegmentSize());
        }
        if (command.overlapSize() != null) {
            strategy.setOverlapSize(command.overlapSize());
        }
        validateSegmentSize(strategy.getMaxSegmentSize(), strategy.getOverlapSize());
        if (Boolean.TRUE.equals(strategy.getIsDefault()) && !chunkType.executable()) {
            throw badRequest("默认策略必须使用已实现的分块类型");
        }
        chunkStrategyRepository.updateById(strategy);
        log.info("分块策略已更新: id={}, name={}", id, strategy.getStrategyName());
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteStrategy(Long id) {
        DocumentChunkStrategyDO strategy = findStrategyById(id);
        chunkStrategyRepository.deleteById(id);
        log.info("分块策略已删除: id={}, name={}", id, strategy.getStrategyName());
    }

    @Transactional(rollbackFor = Exception.class)
    public void setDefaultStrategy(Long id) {
        DocumentChunkStrategyDO strategy = findStrategyById(id);
        ChunkTypeEnum type = validateChunkType(strategy.getChunkType());
        if (!type.executable()) {
            throw badRequest("当前分块类型暂未实现，不能设为默认策略: " + strategy.getChunkType());
        }

        chunkStrategyRepository.clearDefault();
        strategy.setIsDefault(true);
        chunkStrategyRepository.updateById(strategy);
        log.info("分块策略已设为默认: id={}, name={}, type={}",
                id, strategy.getStrategyName(), strategy.getChunkType());
    }

    public ChunkStrategy resolveDefaultChunkStrategy() {
        DocumentChunkStrategyDO strategy = chunkStrategyRepository.findDefault();
        if (strategy == null) {
            return buildFallbackStrategy();
        }
        return buildChunkStrategy(
                strategy.getStrategyName(),
                strategy.getChunkType(),
                strategy.getMaxSegmentSize(),
                strategy.getOverlapSize());
    }

    public ChunkStrategy resolveChunkStrategy(Long chunkStrategyId) {
        DocumentChunkStrategyDO strategy = findStrategyById(chunkStrategyId);
        return buildChunkStrategy(
                strategy.getStrategyName(),
                strategy.getChunkType(),
                strategy.getMaxSegmentSize(),
                strategy.getOverlapSize());
    }

    private ChunkStrategy buildFallbackStrategy() {
        String strategy = chunkingProperties.getStrategy();
        ChunkingProperties.SplitterProperties active = chunkingProperties.active();
        return switch (strategy) {
            case "sentence" -> new SentenceChunkStrategy(
                    "sentence", active.getMaxSegmentSize(), active.getOverlapSize());
            case "fixed" -> new CharacterChunkStrategy(
                    "fixed", active.getMaxSegmentSize(), active.getOverlapSize());
            case "paragraph" -> new ParagraphChunkStrategy(
                    "paragraph", active.getMaxSegmentSize(), active.getOverlapSize());
            default -> new ParagraphChunkStrategy(
                    "paragraph",
                    chunkingProperties.getParagraph().getMaxSegmentSize(),
                    chunkingProperties.getParagraph().getOverlapSize());
        };
    }

    private ChunkStrategy buildChunkStrategy(String strategyName, String chunkType,
                                             int maxSegmentSize, int overlapSize) {
        ChunkTypeEnum type = validateChunkType(chunkType);
        return switch (type) {
            case PARAGRAPH -> new ParagraphChunkStrategy(strategyName, maxSegmentSize, overlapSize);
            case SENTENCE -> new SentenceChunkStrategy(strategyName, maxSegmentSize, overlapSize);
            case CHARACTER -> new CharacterChunkStrategy(strategyName, maxSegmentSize, overlapSize);
            case CONTENT_OWNERSHIP, SEMANTIC -> throw new BaseException(
                    ErrorCodeEnum.DOCUMENT_CHUNK_FAILED.getCode(),
                    "当前分块类型暂未实现: " + chunkType);
        };
    }

    private DocumentChunkStrategyDO findStrategyById(Long id) {
        DocumentChunkStrategyDO strategy = id != null ? chunkStrategyRepository.findById(id) : null;
        if (strategy == null) {
            throw badRequest("分块策略不存在");
        }
        return strategy;
    }

    private ChunkTypeEnum validateChunkType(String chunkType) {
        try {
            return ChunkTypeEnum.fromCode(chunkType);
        } catch (IllegalArgumentException e) {
            throw badRequest("无效的分块类型: " + chunkType);
        }
    }

    private void validateSegmentSize(Integer maxSegmentSize, Integer overlapSize) {
        if (maxSegmentSize == null || maxSegmentSize < 1) {
            throw badRequest("最大字符数必须大于 0");
        }
        if (overlapSize == null || overlapSize < 0) {
            throw badRequest("重叠字符数不能小于 0");
        }
        if (overlapSize >= maxSegmentSize) {
            throw badRequest("重叠字符数必须小于最大字符数");
        }
    }

    private BaseException badRequest(String message) {
        return new BaseException(ErrorCodeEnum.BAD_REQUEST.getCode(), message);
    }
}
