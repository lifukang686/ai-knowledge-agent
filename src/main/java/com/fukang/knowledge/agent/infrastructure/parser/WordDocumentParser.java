package com.fukang.knowledge.agent.infrastructure.parser;

import com.fukang.knowledge.agent.domain.knowledge.model.DocumentParseResult;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ooxml.POIXMLProperties.CoreProperties;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Word 文档解析器
 * <p>基于 Apache POI 实现 .docx 格式 Word 文档的文本提取。
 * 支持提取段落内容和文档属性（标题、作者等），
 * 按段落顺序输出文本，段落之间以换行符分隔</p>
 */
@Slf4j
public class WordDocumentParser implements DocumentParser {

    private static final Set<String> EXTENSIONS = Set.of("docx", "doc");

    @Override
    public Set<String> supportedExtensions() {
        return EXTENSIONS;
    }

    /**
     * 解析 Word 文档
     * <p>使用 POI XWPF 读取 .docx 文档，提取所有段落文本。
     * 段落之间使用换行符分隔，保持文档原始段落结构</p>
     *
     * @param inputStream    Word 文件输入流
     * @param fileName       原始文件名
     * @param fileSizeInBytes 文件大小
     * @return 解析结果，包含提取的文本和元数据
     * @throws BaseException 解析失败时抛出 DOCUMENT_PARSE_FAILED
     */
    @Override
    public DocumentParseResult parse(InputStream inputStream, String fileName, long fileSizeInBytes) {
        log.info("开始解析 Word 文档: fileName={}, size={} bytes", fileName, fileSizeInBytes);

        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            Map<String, String> metadata = extractMetadata(document, fileName, fileSizeInBytes);
            String content = extractText(document);

            DocumentParseResult result = new DocumentParseResult(
                    content, fileName, extractExtension(fileName), metadata, LocalDateTime.now()
            );

            log.info("Word 文档解析完成: fileName={}, paragraphs={}, chars={}",
                    fileName, document.getParagraphs().size(), result.characterCount());
            return result;
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("Word 文档解析失败: fileName={}", fileName, e);
            throw new BaseException(ErrorCodeEnum.DOCUMENT_PARSE_FAILED);
        }
    }

    /**
     * 提取 Word 文档的文本内容
     * <p>遍历所有段落并拼接文本，保留段落结构</p>
     *
     * @param document Word 文档对象
     * @return 提取的全文文本
     */
    private String extractText(XWPFDocument document) {
        List<XWPFParagraph> paragraphs = document.getParagraphs();
        return paragraphs.stream()
                .map(XWPFParagraph::getText)
                .filter(text -> !text.isBlank())
                .collect(Collectors.joining("\n"));
    }

    /**
     * 提取 Word 文档的元数据信息
     * <p>收集文档核心属性、段落数和文件大小等信息</p>
     *
     * @param document      Word 文档对象
     * @param fileName      原始文件名
     * @param fileSizeInBytes 文件大小（字节）
     * @return 元数据键值对
     */
    private Map<String, String> extractMetadata(XWPFDocument document, String fileName, long fileSizeInBytes) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("fileName", fileName);
        metadata.put("paragraphCount", String.valueOf(document.getParagraphs().size()));
        metadata.put("fileSizeInBytes", String.valueOf(fileSizeInBytes));

        if (document.getProperties().getCoreProperties() != null) {
            CoreProperties coreProps = document.getProperties().getCoreProperties();
            if (coreProps.getTitle() != null && !coreProps.getTitle().isBlank()) {
                metadata.put("title", coreProps.getTitle());
            }
            if (coreProps.getCreator() != null && !coreProps.getCreator().isBlank()) {
                metadata.put("author", coreProps.getCreator());
            }
        }
        return metadata;
    }

    /**
     * 提取文件名扩展名
     */
    private String extractExtension(String fileName) {
        if (fileName == null) {
            return "docx";
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == fileName.length() - 1) {
            return "docx";
        }
        return fileName.substring(dotIndex + 1).toLowerCase();
    }
}