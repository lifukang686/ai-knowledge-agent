package com.fukang.knowledge.agent.application.servicedesk.agent;

import com.fukang.knowledge.agent.application.rag.RagAppService;
import com.fukang.knowledge.agent.application.rag.result.QaResult;
import com.fukang.knowledge.agent.application.rag.stream.QaStreamHandler;
import com.fukang.knowledge.agent.application.servicedesk.ServiceDeskStreamHandler;
import com.fukang.knowledge.agent.infrastructure.tool.LocalMethodTool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 服务台知识问答工具：复用现有 RAG 链路回答 IT/HR 知识问题。
 */
@Component
@RequiredArgsConstructor
public class ServiceDeskKnowledgeQaTool implements LocalMethodTool {

    private static final long STREAM_WAIT_TIMEOUT_SECONDS = 115L;
    private static final String NO_RESULT_HINT = "\n\n如果这个问题比较紧急，或者知识库没有覆盖到你的场景，"
            + "可以补充现象、影响范围和发生时间，我可以继续帮你生成工单草稿。";

    private final RagAppService ragAppService;

    @Override
    public String name() {
        return ServiceDeskToolNames.KNOWLEDGE_QA;
    }

    @Override
    public Object execute(Map<String, Object> arguments) {
        ServiceDeskAgentContext context = ServiceDeskAgentContextHolder.getRequired();
        String question = text(arguments, "question", context.question());
        if (context.streamHandler() != null) {
            // SSE 场景复用 RAG 流式链路，把增量 token 直接透传给服务台前端。
            return executeStream(question, context);
        }
        // 非流式调用直接取完整 RAG 结果。
        QaResult result = ragAppService.answer(question, context.knowledgeBaseId(), context.conversationId());
        return toPayload(result, withNoResultHint(result), false);
    }

    private Object executeStream(String question, ServiceDeskAgentContext context) {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<QaResult> resultRef = new AtomicReference<>();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        ServiceDeskStreamHandler serviceDeskHandler = context.streamHandler();

        ragAppService.answerStream(question, context.knowledgeBaseId(), context.conversationId(), new QaStreamHandler() {
            @Override
            public void onStage(String stage, String message) {
                // 给 RAG 阶段加前缀，避免和服务台 Agent 阶段混淆。
                serviceDeskHandler.onStage("rag_" + stage, message);
            }

            @Override
            public void onToken(String token) {
                serviceDeskHandler.onToken(token);
            }

            @Override
            public void onDone(QaResult result) {
                resultRef.set(result);
                done.countDown();
            }

            @Override
            public void onError(String message, Throwable error) {
                errorRef.set(error != null ? error : new IllegalStateException(message));
                done.countDown();
            }
        });

        awaitStream(done);
        if (errorRef.get() != null) {
            throw new IllegalStateException("服务台知识问答流式生成失败", errorRef.get());
        }
        QaResult result = resultRef.get();
        if (result == null) {
            throw new IllegalStateException("服务台知识问答流式生成未返回结果");
        }
        return toPayload(result, result.answer(), true);
    }

    private void awaitStream(CountDownLatch done) {
        try {
            // Agent 工具需要等待 RAG 完成，才能把完整结果作为 observation 返回。
            if (!done.await(STREAM_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new IllegalStateException("服务台知识问答流式生成超时");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("服务台知识问答流式生成被中断", e);
        }
    }

    private String withNoResultHint(QaResult result) {
        String answer = result.answer();
        return "no_results".equalsIgnoreCase(result.status()) ? answer + NO_RESULT_HINT : answer;
    }

    private Map<String, Object> toPayload(QaResult result, String answer, boolean streamedTokens) {
        return Map.of(
                "answer", answer != null ? answer : "",
                "status", result.status() != null ? result.status() : "",
                "conversationId", result.conversationId() != null ? result.conversationId() : "",
                "rewrittenQuery", result.rewrittenQuery() != null ? result.rewrittenQuery() : "",
                "streamedTokens", streamedTokens
        );
    }

    private String text(Map<String, Object> args, String key, String fallback) {
        Object value = args != null ? args.get(key) : null;
        return value != null && !String.valueOf(value).isBlank() ? String.valueOf(value) : fallback;
    }
}
