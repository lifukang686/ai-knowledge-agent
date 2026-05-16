package com.fukang.knowledge.agent.infrastructure.parser;

import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文档解析器工厂
 * <p>根据文件扩展名选择合适的文档解析器。采用注册制，
 * 新增解析器时只需注册即可，无需修改工厂逻辑</p>
 */
@Slf4j
public class DocumentParserFactory {

    private final Map<String, DocumentParser> parserRegistry;
    private final List<DocumentParser> defaultParsers;

    /**
     * 构造解析器工厂并注册默认解析器
     * <p>默认注册 PDF、Word、TXT 三种格式的解析器。
     * 支持后续通过 {@link #registerParser} 动态添加更多格式</p>
     */
    public DocumentParserFactory() {
        this.parserRegistry = new ConcurrentHashMap<>();
        this.defaultParsers = List.of(
                new PdfDocumentParser(),
                new WordDocumentParser(),
                new TxtDocumentParser()
        );

        defaultParsers.forEach(this::registerParser);
        log.info("文档解析器工厂已初始化，已注册 {} 种格式解析器", defaultParsers.size());
    }

    /**
     * 注册自定义文档解析器
     * <p>将解析器支持的所有扩展名注册到工厂中，覆盖同名扩展名的旧解析器</p>
     *
     * @param parser 文档解析器实例
     */
    public void registerParser(DocumentParser parser) {
        for (String extension : parser.supportedExtensions()) {
            parserRegistry.put(extension.toLowerCase(), parser);
            log.info("已注册文档解析器: extension={}, parser={}", extension, parser.getClass().getSimpleName());
        }
    }

    /**
     * 根据文件扩展名获取对应的文档解析器
     *
     * @param extension 文件扩展名（小写）
     * @return 对应的文档解析器实例
     * @throws BaseException 扩展名没有对应解析器时抛出 FILE_TYPE_NOT_SUPPORTED
     */
    public DocumentParser getParser(String extension) {
        if (extension == null || extension.isBlank()) {
            log.warn("文件扩展名为空，无法获取解析器");
            throw new BaseException(ErrorCodeEnum.FILE_TYPE_NOT_SUPPORTED);
        }

        String normalizedExtension = extension.toLowerCase();
        DocumentParser parser = parserRegistry.get(normalizedExtension);

        if (parser == null) {
            log.warn("未找到支持的文档解析器: extension={}", normalizedExtension);
            throw new BaseException(ErrorCodeEnum.FILE_TYPE_NOT_SUPPORTED);
        }

        return parser;
    }

    /**
     * 检查指定扩展名是否有对应的解析器
     *
     * @param extension 文件扩展名
     * @return 存在对应解析器时返回 true
     */
    public boolean supportsExtension(String extension) {
        return extension != null && parserRegistry.containsKey(extension.toLowerCase());
    }
}