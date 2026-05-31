package com.fukang.knowledge.agent.infrastructure.ai;

import com.fukang.knowledge.agent.application.ai.port.ChatCompletionPort;
import com.fukang.knowledge.agent.application.ai.port.StreamingChatCompletionPort;
import com.fukang.knowledge.agent.common.enums.ModelTypeEnum;
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
public class LangChain4jStreamingChatCompletionAdapter implements StreamingChatCompletionPort {

    private final DynamicModelManager dynamicModelManager;

    @Override
    public void completeStream(List<ChatCompletionPort.Message> messages, StreamHandler handler) {
        StreamingChatLanguageModel chatModel = dynamicModelManager.getStreamingChatModel(ModelTypeEnum.CHAT);
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

    private List<ChatMessage> toLangChainMessages(List<ChatCompletionPort.Message> messages) {
        return messages.stream()
                .map(this::toLangChainMessage)
                .toList();
    }

    private ChatMessage toLangChainMessage(ChatCompletionPort.Message message) {
        return switch (message.role()) {
            case SYSTEM -> SystemMessage.from(message.content());
            case USER -> UserMessage.from(message.content());
            case AI -> AiMessage.from(message.content());
        };
    }
}
