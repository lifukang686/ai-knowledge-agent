package com.fukang.knowledge.agent.module.modelruntime.service.client;

import com.fukang.knowledge.agent.module.modelruntime.model.vo.ChatMessage;
import java.util.List;

/**
 * Chat 模型调用端口。
 * <p>应用层只表达消息与返回文本，不直接依赖具体 LLM SDK。</p>
 */
public interface ChatCompletionClient {

    /**
     * 调用 Chat 模型生成完整文本。
     */
    String complete(List<ChatMessage> messages);
}
