package com.fukang.knowledge.agent.application.rag;

import com.fukang.knowledge.agent.application.ai.port.ChatCompletionPort;
import com.fukang.knowledge.agent.application.ai.port.StreamingChatCompletionPort;
import com.fukang.knowledge.agent.application.rag.result.QaResult;
import com.fukang.knowledge.agent.application.rag.stream.QaStreamHandler;
import com.fukang.knowledge.agent.common.context.UserContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * RAG 流式输出服务，负责收集模型 token、转发事件并保存本轮会话结果。
 */
@Service
@RequiredArgsConstructor
public class RagStreamingService {

    private final StreamingChatCompletionPort streamingChatCompletionPort;
    private final RagConversationService ragConversationService;

    /**
     * 执行流式模型调用，并在完成或失败时写入会话记忆。
     */
    public void stream(List<ChatCompletionPort.Message> messages,
                       String originalQuestion,
                       String rewrittenQuery,
                       String status,
                       Long conversationId,
                       QaStreamHandler handler) {
        StringBuilder answer = new StringBuilder();
        Long userId = UserContextHolder.getUserId();
        streamingChatCompletionPort.completeStream(messages, new StreamingChatCompletionPort.StreamHandler() {
            @Override
            public void onToken(String token) {
                answer.append(token);
                handler.onToken(token);
            }

            @Override
            public void onComplete(String fullText) {
                String finalAnswer = fullText != null && !fullText.isBlank()
                        ? fullText
                        : answer.toString();
                runWithUserContext(userId, () -> {
                    ragConversationService.saveTurn(conversationId, originalQuestion, rewrittenQuery, finalAnswer, status);
                    handler.onDone(new QaResult(finalAnswer, rewrittenQuery, status, conversationId));
                });
            }

            @Override
            public void onError(Throwable error) {
                runWithUserContext(userId, () -> {
                    ragConversationService.saveUserFailure(conversationId, originalQuestion, rewrittenQuery);
                    handler.onError("生成失败，请稍后重试", error);
                });
            }
        });
    }

    /**
     * 流式模型回调可能切换线程，保存会话前恢复用户上下文。
     */
    private void runWithUserContext(Long userId, Runnable action) {
        if (userId != null) {
            UserContextHolder.setUserId(userId);
        }
        try {
            action.run();
        } finally {
            if (userId != null) {
                UserContextHolder.clear();
            }
        }
    }
}
