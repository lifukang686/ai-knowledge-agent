package com.fukang.knowledge.agent.application.ai.port;

import java.util.List;

/**
 * 流式 Chat 模型调用端口。
 * <p>应用层通过回调接收 token，不依赖具体 LLM SDK。</p>
 */
public interface StreamingChatCompletionPort {

    /**
     * 调用流式 Chat 模型。
     */
    void completeStream(List<ChatCompletionPort.Message> messages, StreamHandler handler);

    /**
     * 流式模型回调。
     */
    interface StreamHandler {
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
}
