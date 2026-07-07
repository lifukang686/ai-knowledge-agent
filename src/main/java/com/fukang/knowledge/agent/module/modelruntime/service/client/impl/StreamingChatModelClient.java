package com.fukang.knowledge.agent.module.modelruntime.service.client.impl;

import com.fukang.knowledge.agent.module.modelruntime.service.client.ChatCompletionClient;
import com.fukang.knowledge.agent.module.modelruntime.service.manager.DynamicModelManager;
import com.fukang.knowledge.agent.module.modelruntime.service.client.StreamingChatCompletionClient;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * LangChain4j 流式 Chat 模型适配器。
 */
@Component
@RequiredArgsConstructor
public class StreamingChatModelClient implements StreamingChatCompletionClient {

    private final DynamicModelManager dynamicModelManager;

    @Override
    public void completeStream(List<ChatCompletionClient.Message> messages, StreamHandler handler) {
        StreamingChatLanguageModel chatModel = dynamicModelManager.getStreamingChatModel();
        StringBuilder fullText = new StringBuilder();
        chatModel.generate(toLangChainMessages(messages), new StreamingResponseHandler<>() {
            @Override
            public void onNext(String token) {
                fullText.append(token);
                handler.onToken(token);
            }

            @Override
            public void onComplete(Response<AiMessage> response) {
                String text = response != null && response.content() != null
                        ? response.content().text()
                        : fullText.toString();
                handler.onComplete(text != null ? text : fullText.toString());
            }

            @Override
            public void onError(Throwable error) {
                handler.onError(error);
            }
        });
    }

    private List<ChatMessage> toLangChainMessages(List<ChatCompletionClient.Message> messages) {
        return messages.stream()
                .map(this::toLangChainMessage)
                .toList();
    }

    private ChatMessage toLangChainMessage(ChatCompletionClient.Message message) {
        return switch (message.getRole()) {
            case SYSTEM -> SystemMessage.from(message.getContent());
            case USER -> UserMessage.from(message.getContent());
            case AI -> AiMessage.from(message.getContent());
        };
    }
}
