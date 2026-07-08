package com.fukang.knowledge.agent.module.modelruntime.service.client;

/**
 * 流式 Chat 模型回调。
 */
public interface StreamingChatHandler {
    /**
     * 接收增量 token。
     */
    void onToken(String token);

    /**
     * 接收完整文本。
     */
    void onComplete(String fullText);

    /**
     * 接收模型调用异常。
     */
    void onError(Throwable error);
}
