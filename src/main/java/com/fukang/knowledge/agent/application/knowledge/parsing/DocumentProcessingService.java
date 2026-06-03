package com.fukang.knowledge.agent.application.knowledge.parsing;

import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.domain.knowledge.model.ChunkResult;
import com.fukang.knowledge.agent.domain.knowledge.model.DocumentParseResult;
import com.fukang.knowledge.agent.infrastructure.chunk.ChunkStrategy;
import com.fukang.knowledge.agent.infrastructure.chunk.Langchain4jChunkStrategy;
import com.fukang.knowledge.agent.infrastructure.config.ChunkingProperties;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.parser.apache.poi.ApachePoiDocumentParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 文档处理服务。
 * <p>统一使用 LangChain4j 解析上传文件，并按当前分块策略切分为文档块。</p>
 */
@Slf4j
@Service
public class DocumentProcessingService {

    private final Map<String, DocumentParser> parsersByExtension;
    private final ChunkingProperties chunkingProperties;

    public DocumentProcessingService(ChunkingProperties chunkingProperties) {
        this.parsersByExtension = createLangchain4jParsers();
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
        DocumentParser parser = getParser(extension);

        try (InputStream inputStream = new ByteArrayInputStream(fileBytes)) {
            return parseWithLangchain4j(parser, inputStream, fileName, extension, fileSizeInBytes);
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
        DocumentParser parser = getParser(extension);
        return parseWithLangchain4j(parser, inputStream, fileName, extension, fileSizeInBytes);
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
            log.warn("分块策略为空，降级使用 LangChain4j 默认分块策略");
            return chunkDocument(parseResult, new Langchain4jChunkStrategy(chunkingProperties));
        }

        return strategy.chunk(parseResult);
    }

    private String extractExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private Map<String, DocumentParser> createLangchain4jParsers() {
        Map<String, DocumentParser> parsers = new HashMap<>();
        DocumentParser pdfParser = new ApachePdfBoxDocumentParser();
        DocumentParser officeParser = new ApachePoiDocumentParser();
        DocumentParser textParser = new TextDocumentParser();

        parsers.put("pdf", pdfParser);
        for (String extension : new String[] {"doc", "docx", "xls", "xlsx", "ppt", "pptx"}) {
            parsers.put(extension, officeParser);
        }
        parsers.put("txt", textParser);
        parsers.put("md", textParser);
        return Map.copyOf(parsers);
    }

    private DocumentParser getParser(String extension) {
        if (extension == null || extension.isBlank()) {
            log.warn("文件扩展名为空，无法获取 LangChain4j 解析器");
            throw new BaseException(ErrorCodeEnum.FILE_TYPE_NOT_SUPPORTED);
        }
        DocumentParser parser = parsersByExtension.get(extension);
        if (parser == null) {
            log.warn("LangChain4j 暂不支持该文件类型: extension={}", extension);
            throw new BaseException(ErrorCodeEnum.FILE_TYPE_NOT_SUPPORTED);
        }
        return parser;
    }

    private DocumentParseResult parseWithLangchain4j(DocumentParser parser, InputStream inputStream,
                                                     String fileName, String extension, long fileSizeInBytes) {
        try {
            log.info("使用 LangChain4j 解析文档: fileName={}, extension={}, size={} bytes",
                    fileName, extension, fileSizeInBytes);
            Document document = parser.parse(inputStream);
            Map<String, String> metadata = new HashMap<>();
            metadata.put("fileName", fileName);
            metadata.put("fileExtension", extension);
            metadata.put("fileSizeInBytes", String.valueOf(fileSizeInBytes));
            document.metadata().toMap().forEach((key, value) -> {
                if (key != null && value != null) {
                    metadata.put(key, String.valueOf(value));
                }
            });

            DocumentParseResult result = new DocumentParseResult(
                    document.text(), fileName, extension, metadata, LocalDateTime.now());
            log.info("LangChain4j 文档解析完成: fileName={}, chars={}",
                    fileName, result.characterCount());
            return result;
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("LangChain4j 文档解析失败: fileName={}", fileName, e);
            throw new BaseException(ErrorCodeEnum.DOCUMENT_PARSE_FAILED);
        }
    }
}
