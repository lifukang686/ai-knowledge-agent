package com.fukang.knowledge.agent.application.knowledge.parsing;

import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.domain.knowledge.model.ChunkResult;
import com.fukang.knowledge.agent.domain.knowledge.model.DocumentParseResult;
import com.fukang.knowledge.agent.infrastructure.chunk.ChunkStrategy;
import com.fukang.knowledge.agent.infrastructure.chunk.FixedLengthChunkStrategy;
import com.fukang.knowledge.agent.infrastructure.chunk.Langchain4jChunkStrategy;
import com.fukang.knowledge.agent.infrastructure.config.ChunkingProperties;
import com.fukang.knowledge.agent.infrastructure.parser.DocumentParser;
import com.fukang.knowledge.agent.infrastructure.parser.DocumentParserFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * 文档处理服务。
 * <p>负责将上传文件解析为纯文本，并按当前分块策略切分为文档块，供后续入库和向量化流程使用。</p>
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

    public DocumentParseResult parseDocument(byte[] fileBytes, String fileName, long fileSizeInBytes) {
        if (fileBytes == null || fileBytes.length == 0) {
            log.warn("文档字节数据为空，无法解析: fileName={}", fileName);
            throw new BaseException(ErrorCodeEnum.FILE_EMPTY);
        }

        if (fileName == null || fileName.isBlank()) {
            log.warn("文档文件名为空，无法选择解析器");
            throw new BaseException(ErrorCodeEnum.FILE_NAME_EMPTY);
        }

        String extension = extractExtension(fileName);
        DocumentParser parser = parserFactory.getParser(extension);

        try (InputStream inputStream = new ByteArrayInputStream(fileBytes)) {
            return parser.parse(inputStream, fileName, fileSizeInBytes);
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("文档解析失败: fileName={}", fileName, e);
            throw new BaseException(ErrorCodeEnum.DOCUMENT_PARSE_FAILED);
        }
    }

    public DocumentParseResult parseDocument(InputStream inputStream, String fileName, long fileSizeInBytes) {
        if (inputStream == null) {
            log.warn("文档输入流为空，无法解析: fileName={}", fileName);
            throw new BaseException(ErrorCodeEnum.FILE_EMPTY);
        }

        if (fileName == null || fileName.isBlank()) {
            log.warn("文档文件名为空，无法选择解析器");
            throw new BaseException(ErrorCodeEnum.FILE_NAME_EMPTY);
        }

        String extension = extractExtension(fileName);
        DocumentParser parser = parserFactory.getParser(extension);
        return parser.parse(inputStream, fileName, fileSizeInBytes);
    }

    public ChunkResult chunkDocument(DocumentParseResult parseResult) {
        return chunkDocument(parseResult, new Langchain4jChunkStrategy(chunkingProperties));
    }

    public ChunkResult chunkDocument(DocumentParseResult parseResult, ChunkStrategy strategy) {
        if (parseResult == null) {
            log.warn("文档解析结果为空，无法分块");
            throw new BaseException(ErrorCodeEnum.DOCUMENT_CONTENT_EMPTY);
        }

        if (strategy == null) {
            log.warn("分块策略为空，降级使用固定长度分块策略");
            return chunkDocument(parseResult, new FixedLengthChunkStrategy());
        }

        return strategy.chunk(parseResult);
    }

    private String extractExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1).toLowerCase();
    }
}
