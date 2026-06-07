package com.fukang.knowledge.agent.infrastructure.rag;

import com.fukang.knowledge.agent.domain.rag.service.QueryRewritePort;
import com.fukang.knowledge.agent.infrastructure.ai.DynamicModelManager;
import com.fukang.knowledge.agent.infrastructure.ai.PromptTemplateManager;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * RAG 查询改写服务。
 * <p>支持普通改写和带会话历史的多轮追问改写，失败时回退原始问题。</p>
 */
@Slf4j
@Service
public class QueryRewriteService implements QueryRewritePort {

    private static final int MIN_QUERY_LENGTH = 3;
    private static final int MAX_RESULT_LENGTH = 500;
    private static final String ABSTRACTIVE_TEMPLATE = "rag/query-rewrite-abstractive.v1";
    private static final String EXTRACTIVE_TEMPLATE = "rag/query-rewrite-extractive.v1";
    private static final String HYBRID_TEMPLATE = "rag/query-rewrite-hybrid.v1";
    private static final String HISTORY_TEMPLATE = "rag/query-rewrite-with-history.v1";

    private final DynamicModelManager dynamicModelManager;
    private final PromptTemplateManager promptTemplateManager;

    public QueryRewriteService(DynamicModelManager dynamicModelManager,
                               PromptTemplateManager promptTemplateManager) {
        this.dynamicModelManager = dynamicModelManager;
        this.promptTemplateManager = promptTemplateManager;
    }

    @Override
    public String rewrite(String originalQuery) {
        if (originalQuery == null || originalQuery.trim().length() <= MIN_QUERY_LENGTH) {
            log.debug("查询过短，跳过改写");
            return originalQuery;
        }
        return rewriteAbstractive(originalQuery.trim());
    }

    @Override
    public String rewriteWithHistory(String originalQuery, String conversationSummary, String conversationHistory) {
        return rewriteWithHistory(originalQuery, conversationSummary, conversationHistory, "");
    }

    @Override
    public String rewriteWithHistory(String originalQuery,
                                     String conversationSummary,
                                     String conversationHistory,
                                     String userMemory) {
        if ((conversationSummary == null || conversationSummary.isBlank())
                && (conversationHistory == null || conversationHistory.isBlank())
                && (userMemory == null || userMemory.isBlank())) {
            return rewrite(originalQuery);
        }
        return doRewriteWithMessages(originalQuery, "history", List.of(
                SystemMessage.from("你是 RAG 查询改写助手，负责把多轮追问改写成可独立检索的问题。"),
                promptTemplateManager.renderUser(HISTORY_TEMPLATE, Map.of(
                        "summary", conversationSummary != null ? conversationSummary : "",
                        "history", conversationHistory != null ? conversationHistory : "",
                        "userMemory", userMemory != null ? userMemory : "",
                        "question", originalQuery != null ? originalQuery : ""
                ))
        ));
    }

    @Override
    public String rewriteAbstractive(String originalQuery) {
        return doRewrite(originalQuery, ABSTRACTIVE_TEMPLATE, "abstractive");
    }

    @Override
    public String rewriteExtractive(String originalQuery) {
        return doRewrite(originalQuery, EXTRACTIVE_TEMPLATE, "extractive");
    }

    @Override
    public String rewriteHybrid(String originalQuery) {
        return doRewrite(originalQuery, HYBRID_TEMPLATE, "hybrid");
    }

    private String doRewrite(String originalQuery, String templatePath, String strategyName) {
        return doRewriteWithMessages(originalQuery, strategyName, List.of(
                promptTemplateManager.renderSystem(templatePath, null),
                UserMessage.from(originalQuery)
        ));
    }

    private String doRewriteWithMessages(String originalQuery, String strategyName, List<ChatMessage> messages) {
        long start = System.currentTimeMillis();
        log.info("开始查询改写: strategy={}", strategyName);

        ChatLanguageModel chatModel;
        try {
            chatModel = dynamicModelManager.getChatModel();
        } catch (Exception e) {
            log.warn("获取 ChatModel 失败，回退原始查询: strategy={}, error={}", strategyName, e.getMessage());
            return originalQuery;
        }

        try {
            Response<AiMessage> response = chatModel.generate(messages);
            String rewritten = response.content().text();
            if (rewritten == null || rewritten.isBlank()) {
                log.warn("LLM 返回空改写结果，回退原始查询: strategy={}", strategyName);
                return originalQuery;
            }
            if (rewritten.length() > MAX_RESULT_LENGTH) {
                rewritten = rewritten.substring(0, MAX_RESULT_LENGTH);
            }
            log.info("查询改写完成: strategy={}, elapsedMs={}", strategyName, System.currentTimeMillis() - start);
            log.debug("改写前: {}, 改写后: {}", originalQuery, rewritten);
            return rewritten.trim();
        } catch (Exception e) {
            log.warn("查询改写失败，回退原始查询: strategy={}, elapsedMs={}, error={}",
                    strategyName, System.currentTimeMillis() - start, e.getMessage());
            return originalQuery;
        }
    }
}
