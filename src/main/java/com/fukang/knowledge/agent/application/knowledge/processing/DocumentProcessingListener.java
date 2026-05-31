package com.fukang.knowledge.agent.application.knowledge.processing;

import com.fukang.knowledge.agent.domain.knowledge.event.DocumentUploadedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 文档上传事件监听器。
 * <p>上传事务提交后异步触发处理管道，避免上传接口被解析和向量化阻塞。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentProcessingListener {

    private final DocumentProcessingPipeline pipeline;

    /**
     * 上传事务提交后，异步启动文档处理。
     */
    @Async("documentProcessingExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDocumentUploaded(DocumentUploadedEvent event) {
        log.info("收到文档上传事件，开始异步处理: documentId={}, fileName={}",
                event.getDocumentId(), event.getFileName());
        try {
            pipeline.execute(event.getDocumentId(), event.getKnowledgeBaseId(),
                    event.getFilePath(), event.getFileName());
        } catch (Exception e) {
            log.error("文档处理管道执行异常: documentId={}", event.getDocumentId(), e);
        }
    }
}
