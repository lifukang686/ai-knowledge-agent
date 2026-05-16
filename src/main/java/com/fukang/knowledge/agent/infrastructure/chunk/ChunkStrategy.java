package com.fukang.knowledge.agent.infrastructure.chunk;

import com.fukang.knowledge.agent.application.knowledge.model.ChunkResult;
import com.fukang.knowledge.agent.application.knowledge.model.DocumentParseResult;

/**
 * 文档分块策略接口
 * <p>定义统一的分块契约，各分块策略实现此接口以提供不同的文本切分能力。
 * 采用策略模式，支持后续扩展更多分块算法（语义分割、滑动窗口等）</p>
 */
public interface ChunkStrategy {

    /**
     * 对解析后的文档内容执行分块处理
     *
     * @param parseResult 文档解析结果，包含纯文本内容和元数据
     * @return 分块结果，包含分块列表和策略统计信息
     * @throws com.fukang.knowledge.agent.common.exception.BaseException 分块失败时抛出 DOCUMENT_CHUNK_FAILED
     */
    ChunkResult chunk(DocumentParseResult parseResult);

    /**
     * 返回此分块策略的名称
     *
     * @return 策略名称，如 "fixed-length"、"sentence" 等
     */
    String strategyName();
}