package com.fukang.knowledge.agent.module.knowledge.model.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 文档解析结果
 * <p>封装文档解析后提取的纯文本内容和相关元数据，
 * 作为解析阶段与分块阶段之间的数据传递对象</p>
 *
 * @param content       文档解析后的纯文本内容
 * @param title         文档标题（原始文件名）
 * @param fileExtension 文件扩展名（小写），如 pdf、docx、txt
 * @param metadata      文档级元数据，包含文件大小、页数、作者等信息
 * @param parseTime     解析完成时间
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentParseResult {
    private String content;

    private String title;

    private String fileExtension;

    private Map<String, String> metadata;

    private LocalDateTime parseTime;

    /**
     * 检查解析内容是否为空
     *
     * @return 内容为空或仅含空白字符时返回 true
     */
    public boolean isEmpty() {
        return content == null || content.isBlank();
    }

    /**
     * 获取内容的字符长度
     *
     * @return 文档文本的字符数，内容为空时返回 0
     */
    public int characterCount() {
        return content == null ? 0 : content.length();
    }

}