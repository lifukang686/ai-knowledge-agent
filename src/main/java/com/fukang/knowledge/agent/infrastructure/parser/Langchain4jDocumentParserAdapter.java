package com.fukang.knowledge.agent.infrastructure.parser;

import com.fukang.knowledge.agent.application.knowledge.model.DocumentParseResult;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * langchain4j 文档解析器适配器
 * <p>将 langchain4j 内置的 {@link dev.langchain4j.data.document.DocumentParser} 适配为
 * 项目自定义的 {@link DocumentParser} 接口，实现统一的文档解析能力</p>
 */
@Slf4j
public class Langchain4jDocumentParserAdapter implements DocumentParser {

    private final dev.langchain4j.data.document.DocumentParser delegate;
    private final Set<String> extensions;

    public Langchain4jDocumentParserAdapter(dev.langchain4j.data.document.DocumentParser delegate,
                                            List<String> extensions) {
        this.delegate = delegate;
        this.extensions = Set.copyOf(extensions.stream().map(String::toLowerCase).toList());
    }

    @Override
    public DocumentParseResult parse(InputStream inputStream, String fileName, long fileSizeInBytes) {
        log.info("使用 langchain4j 解析文档: fileName={}, size={} bytes", fileName, fileSizeInBytes);

        try {
            dev.langchain4j.data.document.Document langchain4jDoc = delegate.parse(inputStream);
            String content = langchain4jDoc.text();

            Map<String, String> metadata = new HashMap<>();
            metadata.put("fileName", fileName);
            metadata.put("fileSize", String.valueOf(fileSizeInBytes));
            langchain4jDoc.metadata().toMap().forEach((k, v) -> metadata.put(k, v.toString()));

            String extension = fileSizeInBytes >= 0 ? extractExtension(fileName) : "";
            DocumentParseResult result = new DocumentParseResult(
                    content, fileName, extension, metadata, LocalDateTime.now());

            log.info("langchain4j 文档解析完成: fileName={}, chars={}", fileName, result.characterCount());
            return result;
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("langchain4j 文档解析失败: fileName={}", fileName, e);
            throw new BaseException(ErrorCodeEnum.DOCUMENT_PARSE_FAILED);
        }
    }

    @Override
    public Set<String> supportedExtensions() {
        return extensions;
    }

    private String extractExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1).toLowerCase();
    }
}