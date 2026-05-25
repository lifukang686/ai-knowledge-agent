package com.fukang.knowledge.agent.infrastructure.parser;

import com.fukang.knowledge.agent.domain.knowledge.model.DocumentParseResult;

import java.io.InputStream;

/**
 * 文档解析器接口
 * <p>定义统一的文档解析契约，各文档格式解析器实现此接口以提供内容提取能力。
 * 采用策略模式，便于后续扩展新的文档格式支持（如 Markdown、HTML 等）</p>
 */
public interface DocumentParser {

    /**
     * 解析文档输入流，提取纯文本内容和元数据
     *
     * @param inputStream    文档文件的输入流，调用方负责关闭
     * @param fileName       原始文件名，用于判断文件格式和提取标题
     * @param fileSizeInBytes 文件大小（字节），用于元数据记录
     * @return 文档解析结果，包含纯文本内容和元数据
     * @throws com.fukang.knowledge.agent.common.exception.BaseException 解析失败时抛出 DOCUMENT_PARSE_FAILED
     */
    DocumentParseResult parse(InputStream inputStream, String fileName, long fileSizeInBytes);

    /**
     * 返回此解析器支持的文件扩展名列表
     *
     * @return 小写扩展名列表，如 ["pdf"]、["docx", "doc"]
     */
    java.util.Set<String> supportedExtensions();
}