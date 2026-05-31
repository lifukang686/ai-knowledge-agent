package com.fukang.knowledge.agent.application.rag.stream;

import com.fukang.knowledge.agent.application.rag.result.QaResult;

/**
 * RAG 流式问答事件回调。
 */
public interface QaStreamHandler {

    void onStage(String stage, String message);

    void onToken(String token);

    void onDone(QaResult result);

    void onError(String message, Throwable error);
}
