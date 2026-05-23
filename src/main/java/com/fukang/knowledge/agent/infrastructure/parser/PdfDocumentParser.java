package com.fukang.knowledge.agent.infrastructure.parser;

import com.fukang.knowledge.agent.application.knowledge.parsing.model.DocumentParseResult;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * PDF 文档解析器
 * <p>基于 Apache PDFBox 3.x 实现 PDF 文档的文本提取。
 * 支持提取文档元数据（标题、作者、主题、页数等）和全文文本内容，
 * 对于扫描版 PDF（纯图像）无法提取文本，返回内容为空</p>
 */
@Slf4j
public class PdfDocumentParser implements DocumentParser {

    private static final Set<String> EXTENSIONS = Set.of("pdf");

    @Override
    public Set<String> supportedExtensions() {
        return EXTENSIONS;
    }

    /**
     * 解析 PDF 文档
     * <p>使用 PDFBox 逐页提取文本内容，每页文本之间使用换行分隔，
     * 同时收集 PDF 文档属性作为元数据</p>
     *
     * @param inputStream    PDF 文件输入流
     * @param fileName       原始文件名
     * @param fileSizeInBytes 文件大小
     * @return 解析结果，包含提取的文本和元数据
     * @throws BaseException 解析失败时抛出 DOCUMENT_PARSE_FAILED
     */
    @Override
    public DocumentParseResult parse(InputStream inputStream, String fileName, long fileSizeInBytes) {
        log.info("开始解析 PDF 文档: fileName={}, size={} bytes", fileName, fileSizeInBytes);

        try (PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
            Map<String, String> metadata = extractMetadata(document, fileName, fileSizeInBytes);
            String content = extractText(document);

            DocumentParseResult result = new DocumentParseResult(
                    content, fileName, "pdf", metadata, LocalDateTime.now()
            );

            log.info("PDF 文档解析完成: fileName={}, pages={}, chars={}",
                    fileName, document.getNumberOfPages(), result.characterCount());
            return result;
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("PDF 文档解析失败: fileName={}", fileName, e);
            throw new BaseException(ErrorCodeEnum.DOCUMENT_PARSE_FAILED);
        }
    }

    /**
     * 提取 PDF 文档的文本内容
     * <p>按页顺序提取文本，页面之间使用两个换行符分隔，
     * 便于后续分块策略识别页面边界</p>
     *
     * @param document PDF 文档对象
     * @return 提取的全文文本
     * @throws Exception PDFBox 文本提取异常
     */
    private String extractText(PDDocument document) throws Exception {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setSortByPosition(true);
        stripper.setParagraphStart("\n");
        stripper.setPageEnd("\n\n");
        return stripper.getText(document);
    }

    /**
     * 提取 PDF 文档的元数据信息
     * <p>收集文档属性（标题、作者、主题）、页数、文件大小等信息</p>
     *
     * @param document      PDF 文档对象
     * @param fileName      原始文件名
     * @param fileSizeInBytes 文件大小（字节）
     * @return 元数据键值对
     */
    private Map<String, String> extractMetadata(PDDocument document, String fileName, long fileSizeInBytes) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("fileName", fileName);
        metadata.put("pageCount", String.valueOf(document.getNumberOfPages()));
        metadata.put("fileSizeInBytes", String.valueOf(fileSizeInBytes));

        PDDocumentInformation info = document.getDocumentInformation();
        if (info.getTitle() != null && !info.getTitle().isBlank()) {
            metadata.put("title", info.getTitle());
        }
        if (info.getAuthor() != null && !info.getAuthor().isBlank()) {
            metadata.put("author", info.getAuthor());
        }
        if (info.getSubject() != null && !info.getSubject().isBlank()) {
            metadata.put("subject", info.getSubject());
        }
        return metadata;
    }
}