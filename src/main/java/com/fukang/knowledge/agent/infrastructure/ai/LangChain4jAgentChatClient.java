package com.fukang.knowledge.agent.infrastructure.ai;

import com.fukang.knowledge.agent.application.agent.port.AgentChatClient;
import com.fukang.knowledge.agent.common.enums.ModelTypeEnum;
import com.fukang.knowledge.agent.domain.agent.model.AgentChatMessage;
import com.fukang.knowledge.agent.domain.agent.model.AgentChatSession;
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
public class LangChain4jAgentChatClient implements AgentChatClient {

    private static final int DEFAULT_WINDOW_SIZE = 10;

    private final DynamicModelManager dynamicModelManager;

    @Override
    public AgentChatSession newSession(int maxMessages) {
        return new AgentChatSession(maxMessages);
    }

    @Override
    public AgentChatSession newDefaultSession() {
        return new AgentChatSession(DEFAULT_WINDOW_SIZE);
    }

    @Override
    public String generate(AgentChatSession session, List<AgentChatMessage> newMessages) {
        AgentChatSession targetSession = session != null ? session : newDefaultSession();
        if (newMessages != null) {
            newMessages.forEach(targetSession::add);
        }

        ChatLanguageModel chatModel = dynamicModelManager.getChatModel(ModelTypeEnum.CHAT);
        Response<AiMessage> response = chatModel.generate(toLangChainMessages(targetSession.messages()));
        String text = response.content().text();
        targetSession.add(AgentChatMessage.ai(text));
        return text;
    }

    private List<ChatMessage> toLangChainMessages(List<AgentChatMessage> messages) {
        return messages.stream()
                .map(this::toLangChainMessage)
                .toList();
    }

    private ChatMessage toLangChainMessage(AgentChatMessage message) {
        return switch (message.role()) {
            case SYSTEM -> SystemMessage.from(message.content());
            case USER -> UserMessage.from(message.content());
            case AI -> AiMessage.from(message.content());
        };
    }
}
