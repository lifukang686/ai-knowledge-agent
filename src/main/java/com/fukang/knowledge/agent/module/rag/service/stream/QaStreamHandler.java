package com.fukang.knowledge.agent.module.rag.service.stream;

import com.fukang.knowledge.agent.module.rag.model.vo.QaResult;

/**
 * RAG 流式问答事件回调。
 */
public interface QaStreamHandler {

    /** 发送流程阶段事件，例如改写、检索、重排和生成。 */
    void onStage(String stage, String message);

    /** 发送模型生成的增量文本。 */
    void onToken(String token);

    /** 发送最终结果并结束流。 */
    void onDone(QaResult result);

    /** 发送错误事件并结束流。 */
    void onError(String message, Throwable error);
}
