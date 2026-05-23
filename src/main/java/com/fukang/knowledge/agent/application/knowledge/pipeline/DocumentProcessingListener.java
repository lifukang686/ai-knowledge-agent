package com.fukang.knowledge.agent.application.knowledge.pipeline;

import com.fukang.knowledge.agent.application.knowledge.event.DocumentUploadedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 文档上传后处理事件监听器
 * <p>使用 @TransactionalEventListener(phase = AFTER_COMMIT) 确保：
 * <ol>
 *   <li>事件在上传事务成功提交后才被消费，避免读不到文档记录</li>
 *   <li>@Async 使处理在独立线程池中执行，不阻塞上传 API 响应</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentProcessingListener {

    private final DocumentProcessingPipeline pipeline;

    /**
     * 监听文档上传完成事件，异步触发处理管道
     *
     * @param event 文档上传完成事件
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