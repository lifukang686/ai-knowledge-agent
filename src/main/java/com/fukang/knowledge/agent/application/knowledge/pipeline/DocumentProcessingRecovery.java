package com.fukang.knowledge.agent.application.knowledge.pipeline;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.DocumentDO;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.DocumentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 启动时补偿扫描
 * <p>扫描 status = PENDING 的文档（进程重启导致事件丢失），重新触发处理管道。
 * 同时处理中间态文档（PARSING/CHUNKING/EMBEDDING），这些文档在上次进程中被中断，
 * 需要重新从 PENDING 状态开始处理</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentProcessingRecovery {

    private final DocumentMapper documentMapper;
    private final DocumentProcessingPipeline pipeline;

    /**
     * 应用就绪后扫描未完成处理的文档
     */
    @EventListener(ApplicationReadyEvent.class)
    public void recoverPendingDocuments() {
        List<DocumentDO> pendingDocs = documentMapper.selectList(
                new LambdaQueryWrapper<DocumentDO>()
                        .eq(DocumentDO::getStatus, DocumentStatus.PENDING.getCode()));

        if (pendingDocs.isEmpty()) {
            log.info("补偿扫描: 无待处理文档");
        } else {
            log.info("补偿扫描: 发现 {} 个待处理文档，重新提交处理", pendingDocs.size());
            for (DocumentDO doc : pendingDocs) {
                log.info("补偿处理: documentId={}, fileName={}", doc.getId(), doc.getTitle());
                pipeline.execute(doc.getId(), doc.getKnowledgeBaseId(),
                        doc.getFilePath(), doc.getTitle());
            }
        }
    }
}