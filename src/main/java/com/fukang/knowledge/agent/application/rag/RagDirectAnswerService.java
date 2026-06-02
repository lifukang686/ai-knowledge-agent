package com.fukang.knowledge.agent.application.rag;

import com.fukang.knowledge.agent.application.ai.port.ChatCompletionPort;
import com.fukang.knowledge.agent.application.conversation.ConversationMemoryContext;
import com.fukang.knowledge.agent.application.rag.intent.QaIntent;
import com.fukang.knowledge.agent.application.rag.result.QaResult;
import com.fukang.knowledge.agent.application.rag.stream.QaStreamHandler;
import com.fukang.knowledge.agent.infrastructure.ai.PromptTemplateManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * RAG 直接回答服务，负责闲聊、记忆更新等不需要知识库召回的回答分支。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagDirectAnswerService {

    private static final String CHITCHAT_SYSTEM_TEMPLATE = "rag/chitchat-system.v1";
    private static final String DEFAULT_DIRECT_ANSWER = "你好！我是你的知识库问答助手，可以帮你基于已上传文档进行问答。";
    private static final String MEMORY_UPDATE_ANSWER = "好的，我记住了。";

    private final PromptTemplateManager promptTemplateManager;
    private final ChatCompletionPort chatCompletionPort;
    private final RagConversationService ragConversationService;
    private final RagStreamingService ragStreamingService;

    /**
     * 根据意图处理非知识库问题，如闲聊和记忆更新。
     */
    public QaResult answerByIntent(String question, ConversationMemoryContext memory, QaIntent intent) {
        String answer = intent == QaIntent.MEMORY_UPDATE
                ? MEMORY_UPDATE_ANSWER
                : directChat(question, memory);
        ragConversationService.saveSuccessfulTurn(memory.conversationId(), question, question, answer);
        return new QaResult(answer, question, "success", memory.conversationId());
    }

    /**
     * 在检索无结果但判断为闲聊时，降级为直接对话回答。
     */
    public QaResult answerDirectChat(String questionForAnswer,
                                     String originalQuestion,
                                     String rewrittenQuery,
                                     ConversationMemoryContext memory) {
        String answer = directChat(questionForAnswer, memory);
        ragConversationService.saveSuccessfulTurn(memory.conversationId(), originalQuestion, rewrittenQuery, answer);
        return new QaResult(answer, rewrittenQuery, "success", memory.conversationId());
    }

    /**
     * 流式处理非知识库问题。
     */
    public void streamByIntent(String question,
                               ConversationMemoryContext memory,
                               QaIntent intent,
                               QaStreamHandler handler) {
        if (intent == QaIntent.MEMORY_UPDATE) {
            ragConversationService.saveSuccessfulTurn(memory.conversationId(), question, question, MEMORY_UPDATE_ANSWER);
            handler.onToken(MEMORY_UPDATE_ANSWER);
            handler.onDone(new QaResult(MEMORY_UPDATE_ANSWER, question, "success", memory.conversationId()));
            return;
        }
        streamDirectChat(question, question, question, memory, handler);
    }

    public void streamDirectChat(String questionForAnswer,
                                 String originalQuestion,
                                 String rewrittenQuery,
                                 ConversationMemoryContext memory,
                                 QaStreamHandler handler) {
        ragStreamingService.stream(List.of(
                ChatCompletionPort.Message.system(promptTemplateManager.renderText(CHITCHAT_SYSTEM_TEMPLATE, null)),
                ChatCompletionPort.Message.user(ragConversationService.buildDirectChatPrompt(questionForAnswer, memory))
        ), originalQuestion, rewrittenQuery, "success", memory.conversationId(), handler);
    }

    private String directChat(String question, ConversationMemoryContext memory) {
        try {
            String answer = chatCompletionPort.complete(List.of(
                    ChatCompletionPort.Message.system(promptTemplateManager.renderText(CHITCHAT_SYSTEM_TEMPLATE, null)),
                    ChatCompletionPort.Message.user(ragConversationService.buildDirectChatPrompt(question, memory))
            ));
            return answer == null || answer.isBlank() ? DEFAULT_DIRECT_ANSWER : answer;
        } catch (Exception e) {
            log.error("Direct LLM answer failed", e);
            return DEFAULT_DIRECT_ANSWER;
        }
    }
}
