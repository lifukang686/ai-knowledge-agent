package com.fukang.knowledge.agent.api.chunkstrategy;

import com.fukang.knowledge.agent.api.chunkstrategy.dto.ChunkStrategyReq;
import com.fukang.knowledge.agent.api.chunkstrategy.dto.ChunkStrategyResp;
import com.fukang.knowledge.agent.api.chunkstrategy.dto.ChunkStrategyUpdateReq;
import com.fukang.knowledge.agent.application.knowledge.chunk.DocumentChunkStrategyAppService;
import com.fukang.knowledge.agent.application.knowledge.command.CreateChunkStrategyCommand;
import com.fukang.knowledge.agent.application.knowledge.command.UpdateChunkStrategyCommand;
import com.fukang.knowledge.agent.common.result.PageResponse;
import com.fukang.knowledge.agent.common.result.Result;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.DocumentChunkStrategyDO;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 分块策略管理控制器。
 */
@RestController
@RequestMapping("/api/chunk-strategies")
@RequiredArgsConstructor
public class ChunkStrategyController {

    private final DocumentChunkStrategyAppService chunkStrategyAppService;

    @PostMapping
    public Result<Long> createStrategy(@RequestBody @Validated ChunkStrategyReq req) {
        return Result.success(chunkStrategyAppService.createStrategy(
                new CreateChunkStrategyCommand(
                        req.strategyName(),
                        req.chunkType(),
                        req.maxSegmentSize(),
                        req.overlapSize())));
    }

    @PutMapping("/{id}")
    public Result<Void> updateStrategy(@PathVariable("id") Long id,
                                       @RequestBody @Validated ChunkStrategyUpdateReq req) {
        chunkStrategyAppService.updateStrategy(id,
                new UpdateChunkStrategyCommand(
                        req.strategyName(),
                        req.chunkType(),
                        req.maxSegmentSize(),
                        req.overlapSize()));
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteStrategy(@PathVariable("id") Long id) {
        chunkStrategyAppService.deleteStrategy(id);
        return Result.success();
    }

    @GetMapping
    public Result<PageResponse<ChunkStrategyResp>> listStrategies(
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "pageSize", defaultValue = "20") long pageSize,
            @RequestParam(value = "keyword", required = false) String keyword) {
        PageResponse<DocumentChunkStrategyDO> pageResult =
                chunkStrategyAppService.listStrategies(page, pageSize, keyword);
        return Result.success(new PageResponse<>(
                pageResult.getItems().stream().map(this::toResp).toList(),
                pageResult.getTotal(),
                pageResult.getPage(),
                pageResult.getPageSize()));
    }

    @PutMapping("/{id}/default")
    public Result<Void> setDefaultStrategy(@PathVariable("id") Long id) {
        chunkStrategyAppService.setDefaultStrategy(id);
        return Result.success();
    }

    private ChunkStrategyResp toResp(DocumentChunkStrategyDO strategy) {
        return new ChunkStrategyResp(
                strategy.getId(),
                strategy.getStrategyName(),
                strategy.getChunkType(),
                strategy.getMaxSegmentSize(),
                strategy.getOverlapSize(),
                strategy.getIsDefault(),
                strategy.getCreateTime(),
                strategy.getUpdateTime());
    }
}
