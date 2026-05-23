package com.fukang.knowledge.agent.infrastructure.parser;

import com.fukang.knowledge.agent.application.knowledge.parsing.model.DocumentParseResult;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 纯文本文档解析器
 * <p>支持 .txt 和 .md 等纯文本格式文档的内容提取。
 * 按行读取文本内容，保留原始换行结构</p>
 */
@Slf4j
public class TxtDocumentParser implements DocumentParser {

    private static final Set<String> EXTENSIONS = Set.of("txt", "md");

    @Override
    public Set<String> supportedExtensions() {
        return EXTENSIONS;
    }

    /**
     * 解析纯文本文档
     * <p>以 UTF-8 编码按行读取文本内容，保留文档的原始换行结构</p>
     *
     * @param inputStream    文本文件输入流
     * @param fileName       原始文件名
     * @param fileSizeInBytes 文件大小
     * @return 解析结果，包含提取的文本和元数据
     * @throws BaseException 解析失败时抛出 DOCUMENT_PARSE_FAILED
     */
    @Override
    public DocumentParseResult parse(InputStream inputStream, String fileName, long fileSizeInBytes) {
        log.info("开始解析文本文档: fileName={}, size={} bytes", fileName, fileSizeInBytes);

        try {
            String content = readTextContent(inputStream);
            Map<String, String> metadata = buildMetadata(fileName, fileSizeInBytes, content);

            DocumentParseResult result = new DocumentParseResult(
                    content, fileName, extractExtension(fileName), metadata, LocalDateTime.now()
            );

            log.info("文本文档解析完成: fileName={}, lines={}, chars={}",
                    fileName, content.lines().count(), result.characterCount());
            return result;
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("文本文档解析失败: fileName={}", fileName, e);
            throw new BaseException(ErrorCodeEnum.DOCUMENT_PARSE_FAILED);
        }
    }

    /**
     * 读取输入流中的全部文本内容
     * <p>使用 UTF-8 编码按行读取，保持原始换行格式</p>
     *
     * @param inputStream 文件输入流
     * @return 文件文本内容
     * @throws Exception IO 异常
     */
    private String readTextContent(InputStream inputStream) throws Exception {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append('\n');
            }
        }
        return content.toString();
    }

    /**
     * 构建文本文件的元数据
     *
     * @param fileName      原始文件名
     * @param fileSizeInBytes 文件大小（字节）
     * @param content       文件文本内容
     * @return 元数据键值对
     */
    private Map<String, String> buildMetadata(String fileName, long fileSizeInBytes, String content) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("fileName", fileName);
        metadata.put("fileSizeInBytes", String.valueOf(fileSizeInBytes));
        metadata.put("lineCount", String.valueOf(content.lines().count()));
        metadata.put("encoding", StandardCharsets.UTF_8.name());
        return metadata;
    }

    /**
     * 提取文件名扩展名
     */
    private String extractExtension(String fileName) {
        if (fileName == null) {
            return "txt";
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == fileName.length() - 1) {
            return "txt";
        }
        return fileName.substring(dotIndex + 1).toLowerCase();
    }
}