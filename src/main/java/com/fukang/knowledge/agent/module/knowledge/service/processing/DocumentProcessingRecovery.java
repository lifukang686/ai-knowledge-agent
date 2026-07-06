package com.fukang.knowledge.agent.module.knowledge.service.processing;

import com.fukang.knowledge.agent.module.knowledge.mapper.DocumentMapper;
import com.fukang.knowledge.agent.common.enums.DocumentStatusEnum;
import com.fukang.knowledge.agent.module.knowledge.model.entity.DocumentEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 文档处理补偿扫描器。
 * <p>应用启动后重新提交 PENDING 文档，降低上传后进程重启导致事件丢失的风险。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentProcessingRecovery {

    private final DocumentMapper documentMapper;
    private final DocumentProcessingPipeline pipeline;

    /**
     * 应用就绪后扫描未进入处理管道的文档。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void recoverPendingDocuments() {
        List<DocumentEntity> pendingDocs = documentMapper.findByStatus(DocumentStatusEnum.PENDING.getCode());

        if (pendingDocs.isEmpty()) {
            log.info("补偿扫描: 无待处理文档");
            return;
        }

        log.info("补偿扫描: 发现 {} 个待处理文档，重新提交处理", pendingDocs.size());
        for (DocumentEntity doc : pendingDocs) {
            log.info("补偿处理: documentId={}, fileName={}", doc.getId(), doc.getTitle());
            pipeline.execute(doc.getId(), doc.getKnowledgeBaseId(), null, doc.getFilePath(), doc.getTitle());
        }
    }
}
