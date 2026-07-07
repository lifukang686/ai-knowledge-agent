package com.fukang.knowledge.agent.module.modelruntime.service.client.impl;

import com.fukang.knowledge.agent.module.modelruntime.service.client.ChatCompletionClient;
import com.fukang.knowledge.agent.module.modelruntime.service.manager.DynamicModelManager;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * LangChain4j Chat 模型适配器。
 */
@Component
@RequiredArgsConstructor
public class ChatModelClient implements ChatCompletionClient {

    private final DynamicModelManager dynamicModelManager;

    @Override
    public String complete(List<Message> messages) {
        ChatLanguageModel chatModel = dynamicModelManager.getChatModel();
        Response<AiMessage> response = chatModel.generate(toLangChainMessages(messages));
        return response.content().text();
    }

    private List<ChatMessage> toLangChainMessages(List<Message> messages) {
        return messages.stream()
                .map(this::toLangChainMessage)
                .toList();
    }

    private ChatMessage toLangChainMessage(Message message) {
        return switch (message.getRole()) {
            case SYSTEM -> SystemMessage.from(message.getContent());
            case USER -> UserMessage.from(message.getContent());
            case AI -> AiMessage.from(message.getContent());
        };
    }
}
