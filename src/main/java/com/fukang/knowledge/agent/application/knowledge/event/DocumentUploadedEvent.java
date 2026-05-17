package com.fukang.knowledge.agent.application.knowledge.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 文档上传完成事件
 * <p>在上传事务成功提交后发布，触发后续异步处理管道。
 * 携带文档ID、知识库ID、MinIO 文件路径和原始文件名，
 * 供 {@code DocumentProcessingListener} 消费</p>
 */
@Getter
public class DocumentUploadedEvent extends ApplicationEvent {

    private final Long knowledgeBaseId;
    private final String filePath;
    private final String fileName;

    public DocumentUploadedEvent(Object source,
                                 Long documentId,
                                 Long knowledgeBaseId,
                                 String filePath,
                                 String fileName) {
        super(source);
        this.knowledgeBaseId = knowledgeBaseId;
        this.filePath = filePath;
        this.fileName = fileName;
        this.documentId = documentId;
    }

    /** 新创建的文档ID */
    private final Long documentId;
}