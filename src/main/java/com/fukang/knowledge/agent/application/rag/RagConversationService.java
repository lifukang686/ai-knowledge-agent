package com.fukang.knowledge.agent.application.rag;

import com.fukang.knowledge.agent.application.conversation.ConversationMemoryContext;
import com.fukang.knowledge.agent.application.conversation.ConversationMemoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * RAG 会话服务，统一封装问答编排中的会话上下文准备、记忆格式化和消息保存。
 */
@Service
@RequiredArgsConstructor
public class RagConversationService {

    private static final String STATUS_SUCCESS = "success";
    private static final String STATUS_FAILED = "failed";

    private final ConversationMemoryService conversationMemoryService;

    /**
     * 准备本轮问答需要的会话摘要、改写历史和回答历史。
     */
    public ConversationMemoryContext prepareContext(Long conversationId, Long knowledgeBaseId, String question) {
        return conversationMemoryService.prepareContext(conversationId, knowledgeBaseId, question);
    }

    /**
     * 保存一次成功的用户问题和助手回答。
     */
    public void saveSuccessfulTurn(Long conversationId, String question, String rewrittenQuery, String answer) {
        saveTurn(conversationId, question, rewrittenQuery, answer, STATUS_SUCCESS);
    }

    /**
     * 按指定状态保存一轮对话消息。
     */
    public void saveTurn(Long conversationId, String question, String rewrittenQuery, String answer, String status) {
        conversationMemoryService.saveUserMessage(conversationId, question, rewrittenQuery, status);
        conversationMemoryService.saveAssistantMessage(conversationId, answer, status);
    }

    public void saveUserFailure(Long conversationId, String question, String rewrittenQuery) {
        conversationMemoryService.saveUserMessage(conversationId, question, rewrittenQuery, STATUS_FAILED);
    }

    public String buildDirectChatPrompt(String question, ConversationMemoryContext memory) {
        String answerMemory = buildAnswerMemory(memory);
        if (answerMemory.isBlank()) {
            return question;
        }
        return "【会话记忆】\n" + answerMemory + "\n\n【用户问题】\n" + question;
    }

    /**
     * 拼装回答阶段使用的短期记忆文本。
     */
    public String buildAnswerMemory(ConversationMemoryContext memory) {
        StringBuilder builder = new StringBuilder();
        if (memory.summary() != null && !memory.summary().isBlank()) {
            builder.append("摘要：").append(memory.summary()).append("\n");
        }
        if (memory.answerHistory() != null && !memory.answerHistory().isBlank()) {
            builder.append("最近对话：\n").append(memory.answerHistory());
        }
        return builder.toString().trim();
    }
}
