package com.fukang.knowledge.agent.module.modelruntime.service.client;

import com.fukang.knowledge.agent.module.modelruntime.model.vo.ChatMessage;

import java.util.List;

/**
 * 流式 Chat 模型调用端口。
 * <p>应用层通过回调接收 token，不依赖具体 LLM SDK。</p>
 */
public interface StreamingChatCompletionClient {

    /**
     * 调用流式 Chat 模型。
     */
    void completeStream(List<ChatMessage> messages, StreamingChatHandler handler);
}
