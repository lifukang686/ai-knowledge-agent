package com.fukang.knowledge.agent.infrastructure.ai;

import com.fukang.knowledge.agent.application.ai.port.ChatCompletionPort;
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
public class LangChain4jChatCompletionAdapter implements ChatCompletionPort {

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
        return switch (message.role()) {
            case SYSTEM -> SystemMessage.from(message.content());
            case USER -> UserMessage.from(message.content());
            case AI -> AiMessage.from(message.content());
        };
    }
}
