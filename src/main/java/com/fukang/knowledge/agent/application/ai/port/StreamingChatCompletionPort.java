package com.fukang.knowledge.agent.application.ai.port;

import java.util.List;

/**
 * 流式 Chat 模型调用端口。
 * <p>应用层通过回调接收 token，不依赖具体 LLM SDK。</p>
 */
public interface StreamingChatCompletionPort {

    void completeStream(List<ChatCompletionPort.Message> messages, StreamHandler handler);

    interface StreamHandler {
        void onToken(String token);

        void onComplete(String fullText);

        void onError(Throwable error);
    }
}
