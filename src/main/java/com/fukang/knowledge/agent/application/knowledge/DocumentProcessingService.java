package com.fukang.knowledge.agent.application.knowledge;

import com.fukang.knowledge.agent.application.knowledge.model.ChunkResult;
import com.fukang.knowledge.agent.application.knowledge.model.DocumentParseResult;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.infrastructure.chunk.ChunkStrategy;
import com.fukang.knowledge.agent.infrastructure.chunk.FixedLengthChunkStrategy;
import com.fukang.knowledge.agent.infrastructure.chunk.Langchain4jChunkStrategy;
import com.fukang.knowledge.agent.infrastructure.chunk.SentenceChunkStrategy;
import com.fukang.knowledge.agent.infrastructure.parser.DocumentParser;
import com.fukang.knowledge.agent.infrastructure.parser.DocumentParserFactory;
import com.fukang.knowledge.agent.rag.config.ChunkingProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 文档处理服务
 * <p>知识库管理模块的核心公共方法组件，提供文档解析与分块的端到端处理能力。
 * 整合文档解析器和分块策略，对外提供简洁统一的调用接口。
 * 设计为无状态服务，可在平台其他模块中直接注入使用</p>
 *
 * <p>主要职责：
 * <ul>
 *   <li>接收原始文件数据（字节数组或输入流），自动选择合适的解析器提取文本</li>
 *   <li>使用配置的分块策略将文本切分为适合嵌入模型的文本文块</li>
 *   <li>提取并保留文档级和块级元数据，供后续索引和检索使用</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * DocumentProcessingService service;
 *
 * // 从字节数组解析并分块
 * DocumentParseResult parsed = service.parseDocument(fileBytes, "report.pdf", fileSize);
 * ChunkResult chunks = service.chunkDocument(parsed);
 *
 * // 端到端处理：解析 + 分块
 * ChunkResult result = service.parseAndChunk(fileBytes, "report.pdf", fileSize);
 * }</pre>
 *
 * @see DocumentParserFactory
 * @see FixedLengthChunkStrategy
 * @see SentenceChunkStrategy
 * @see Langchain4jChunkStrategy
 */
@Slf4j
@Service
public class DocumentProcessingService {

    private final DocumentParserFactory parserFactory;
    private final ChunkingProperties chunkingProperties;

    public DocumentProcessingService(ChunkingProperties chunkingProperties) {
        this.parserFactory = new DocumentParserFactory();
        this.chunkingProperties = chunkingProperties;
    }

    /**
     * 解析文档字节数据
     * <p>根据文件名自动选择合适的文档解析器，提取纯文本内容和元数据。
     * 内部将字节数组包装为输入流，解析完成后自动关闭流。
     * 此为知识库管理模块中"文档解析"步骤的核心方法</p>
     *
     * @param fileBytes      文档文件的字节数据
     * @param fileName       原始文件名，用于判断文件格式和提取元数据
     * @param fileSizeInBytes 文件大小（字节）
     * @return 文档解析结果，包含纯文本内容和文档级元数据
     * @throws BaseException 文件扩展名不支持时抛出 FILE_TYPE_NOT_SUPPORTED，
     *                       解析失败时抛出 DOCUMENT_PARSE_FAILED
     */
    public DocumentParseResult parseDocument(byte[] fileBytes, String fileName, long fileSizeInBytes) {
        if (fileBytes == null || fileBytes.length == 0) {
            log.warn("文档字节数据为空，无法解析: fileName={}", fileName);
            throw new BaseException(ErrorCodeEnum.FILE_EMPTY);
        }

        if (fileName == null || fileName.isBlank()) {
            log.warn("文件名为空，无法确定文档解析器");
            throw new BaseException(ErrorCodeEnum.FILE_NAME_EMPTY);
        }

        String extension = extractExtension(fileName);
        DocumentParser parser = parserFactory.getParser(extension);

        try (InputStream inputStream = new ByteArrayInputStream(fileBytes)) {
            return parser.parse(inputStream, fileName, fileSizeInBytes);
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("文档解析过程发生异常: fileName={}", fileName, e);
            throw new BaseException(ErrorCodeEnum.DOCUMENT_PARSE_FAILED);
        }
    }

    /**
     * 解析文档输入流
     * <p>从输入流中解析文档内容，适用于流式场景（如直接从网络流读取）。
     * 调用方负责关闭输入流</p>
     *
     * @param inputStream    文档文件的输入流
     * @param fileName       原始文件名
     * @param fileSizeInBytes 文件大小（字节）
     * @return 文档解析结果
     * @throws BaseException 解析失败时抛出对应异常
     */
    public DocumentParseResult parseDocument(InputStream inputStream, String fileName, long fileSizeInBytes) {
        if (inputStream == null) {
            log.warn("文档输入流为空，无法解析: fileName={}", fileName);
            throw new BaseException(ErrorCodeEnum.FILE_EMPTY);
        }

        if (fileName == null || fileName.isBlank()) {
            log.warn("文件名为空，无法确定文档解析器");
            throw new BaseException(ErrorCodeEnum.FILE_NAME_EMPTY);
        }

        String extension = extractExtension(fileName);
        DocumentParser parser = parserFactory.getParser(extension);

        return parser.parse(inputStream, fileName, fileSizeInBytes);
    }

    /**
     * 对已解析的文档执行分块（使用默认固定长度策略）
     * <p>基于默认参数（最大 1000 字符/块、100 字符重叠）对文本进行分块。
     * 适用于大多数通用文档场景</p>
     *
     * @param parseResult 文档解析结果
     * @return 分块结果，包含按序排列的文本文块列表
     * @throws BaseException 文档内容为空时抛出 DOCUMENT_CONTENT_EMPTY
     */
    public ChunkResult chunkDocument(DocumentParseResult parseResult) {
        return chunkDocument(parseResult, new FixedLengthChunkStrategy());
    }

    /**
     * 使用指定策略对已解析的文档执行分块
     * <p>支持灵活切换分块策略，适用于需要不同分块算法的场景</p>
     *
     * @param parseResult 文档解析结果
     * @param strategy    分块策略实例
     * @return 分块结果
     * @throws BaseException 文档内容为空或分块失败时抛出对应异常
     */
    public ChunkResult chunkDocument(DocumentParseResult parseResult, ChunkStrategy strategy) {
        if (parseResult == null) {
            log.warn("文档解析结果为空，无法分块");
            throw new BaseException(ErrorCodeEnum.DOCUMENT_CONTENT_EMPTY);
        }

        if (strategy == null) {
            log.warn("分块策略为空，使用默认固定长度策略");
            return chunkDocument(parseResult, new FixedLengthChunkStrategy());
        }

        return strategy.chunk(parseResult);
    }

    /**
     * 端到端处理：解析文档并按默认策略分块
     * <p>整合文档解析和分块两个步骤，一站式完成从原始文件到文本文块的转换。
     * 使用默认的固定长度分块策略（最大 1000 字符/块，100 字符重叠）。
     * 此为知识库管理模块文档入库流程的核心方法之一</p>
     *
     * @param fileBytes      文档文件的字节数据
     * @param fileName       原始文件名
     * @param fileSizeInBytes 文件大小（字节）
     * @return 分块结果，包含解析元数据和文本文块列表
     * @throws BaseException 文件为空、格式不支持、解析或分块失败时抛出对应异常
     */
    public ChunkResult parseAndChunk(byte[] fileBytes, String fileName, long fileSizeInBytes) {
        return parseAndChunk(fileBytes, fileName, fileSizeInBytes, null);
    }

    /**
     * 端到端处理：解析文档并使用指定策略分块
     * <p>整合文档解析和分块两个步骤，一站式的文档处理方案。
     * 自动选择合适的解析器，然后使用指定的分块策略处理</p>
     *
     * @param fileBytes      文档文件的字节数据
     * @param fileName       原始文件名
     * @param fileSizeInBytes 文件大小（字节）
     * @param strategy       分块策略，为 null 时使用默认固定长度策略
     * @return 分块结果
     * @throws BaseException 文件为空、格式不支持、解析或分块失败时抛出对应异常
     */
    public ChunkResult parseAndChunk(byte[] fileBytes, String fileName, long fileSizeInBytes,
                                     ChunkStrategy strategy) {
        log.info("开始端到端文档处理: fileName={}, size={} bytes, strategy={}",
                fileName, fileSizeInBytes,
                strategy != null ? strategy.strategyName() : "default");

        DocumentParseResult parseResult = parseDocument(fileBytes, fileName, fileSizeInBytes);
        ChunkResult chunkResult = chunkDocument(parseResult,
                strategy != null ? strategy : new FixedLengthChunkStrategy());

        log.info("端到端文档处理完成: fileName={}, chunks={}, strategy={}",
                fileName, chunkResult.totalChunks(), chunkResult.strategyName());
        return chunkResult;
    }

    /**
     * 使用句子级分块策略处理文档
     * <p>适用于对语义完整性要求更高的场景，如论文、合同等长文档。
     * 以句子为基本单位构建文本块</p>
     *
     * @param fileBytes      文档文件的字节数据
     * @param fileName       原始文件名
     * @param fileSizeInBytes 文件大小（字节）
     * @return 分块结果
     */
    public ChunkResult parseAndChunkBySentence(byte[] fileBytes, String fileName, long fileSizeInBytes) {
        return parseAndChunk(fileBytes, fileName, fileSizeInBytes, new SentenceChunkStrategy());
    }

    /**
     * 使用 langchain4j 分割器对已解析的文档执行分块
     * <p>基于配置的 chunking 策略（paragraph/sentence/fixed），
     * 使用 langchain4j 的层次化分割器进行智能分块</p>
     *
     * @param parseResult 文档解析结果
     * @return 分块结果
     */
    public ChunkResult chunkDocumentWithLangchain4j(DocumentParseResult parseResult) {
        return chunkDocument(parseResult, new Langchain4jChunkStrategy(chunkingProperties));
    }

    /**
     * 端到端处理：使用 langchain4j 解析器和分割器
     * <p>使用 langchain4j 内置的文档解析器和分割策略完成完整的文档处理流程</p>
     *
     * @param fileBytes       文档文件的字节数据
     * @param fileName        原始文件名
     * @param fileSizeInBytes 文件大小（字节）
     * @return 分块结果
     */
    public ChunkResult parseAndChunkWithLangchain4j(byte[] fileBytes, String fileName, long fileSizeInBytes) {
        return parseAndChunk(fileBytes, fileName, fileSizeInBytes,
                new Langchain4jChunkStrategy(chunkingProperties));
    }

    /**
     * 便捷方法：使用 langchain4j 策略端到端处理文档
     * <p>等同于 {@link #parseAndChunkWithLangchain4j}，提供语义化别名</p>
     *
     * @param fileBytes       文档文件的字节数据
     * @param fileName        原始文件名
     * @param fileSizeInBytes 文件大小（字节）
     * @return 分块结果
     */
    public ChunkResult parseAndChunkByLangchain4jStrategy(byte[] fileBytes, String fileName, long fileSizeInBytes) {
        return parseAndChunkWithLangchain4j(fileBytes, fileName, fileSizeInBytes);
    }

    /**
     * 仅解析文档（不分块）
     * <p>适用于仅需要提取文档文本内容，不需要进一步分块的场景</p>
     *
     * @param fileBytes      文档文件的字节数据
     * @param fileName       原始文件名
     * @param fileSizeInBytes 文件大小（字节）
     * @return 解析后的文档元数据映射
     */
    public Map<String, Object> parseOnly(byte[] fileBytes, String fileName, long fileSizeInBytes) {
        DocumentParseResult result = parseDocument(fileBytes, fileName, fileSizeInBytes);

        Map<String, Object> response = new HashMap<>();
        response.put("title", result.title());
        response.put("extension", result.fileExtension());
        response.put("content", result.content());
        response.put("characterCount", result.characterCount());
        response.put("metadata", result.metadata());
        response.put("parseTime", result.parseTime());
        return response;
    }

    /**
     * 从文件名中提取扩展名
     *
     * @param fileName 文件名
     * @return 小写的文件扩展名
     */
    private String extractExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1).toLowerCase();
    }
}