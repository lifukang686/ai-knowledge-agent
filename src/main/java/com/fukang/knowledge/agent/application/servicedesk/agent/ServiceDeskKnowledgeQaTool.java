package com.fukang.knowledge.agent.application.servicedesk.agent;

import com.fukang.knowledge.agent.application.rag.RagAppService;
import com.fukang.knowledge.agent.application.rag.result.QaResult;
import com.fukang.knowledge.agent.infrastructure.tool.LocalMethodTool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 服务台知识问答工具：复用现有 RAG 链路回答 IT/HR 知识问题。
 */
@Component
@RequiredArgsConstructor
public class ServiceDeskKnowledgeQaTool implements LocalMethodTool {

    private final RagAppService ragAppService;

    @Override
    public String name() {
        return ServiceDeskToolNames.KNOWLEDGE_QA;
    }

    @Override
    public Object execute(Map<String, Object> arguments) {
        ServiceDeskAgentContext context = ServiceDeskAgentContextHolder.getRequired();
        String question = text(arguments, "question", context.question());
        QaResult result = ragAppService.answer(question, context.knowledgeBaseId(), context.conversationId());
        String answer = result.answer();
        if ("no_results".equalsIgnoreCase(result.status())) {
            answer = answer + "\n\n如果这个问题比较紧急，或者知识库没有覆盖到你的场景，可以补充现象、影响范围和发生时间，我可以继续帮你生成工单草稿。";
        }
        return Map.of(
                "answer", answer,
                "status", result.status(),
                "conversationId", result.conversationId() != null ? result.conversationId() : "",
                "rewrittenQuery", result.rewrittenQuery() != null ? result.rewrittenQuery() : ""
        );
    }

    private String text(Map<String, Object> args, String key, String fallback) {
        Object value = args != null ? args.get(key) : null;
        return value != null && !String.valueOf(value).isBlank() ? String.valueOf(value) : fallback;
    }
}
