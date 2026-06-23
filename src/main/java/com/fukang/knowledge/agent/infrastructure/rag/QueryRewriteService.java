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

    /**
     * 过短问题不做改写。
     */
    private static final int MIN_QUERY_LENGTH = 3;
    /**
     * 改写结果最大长度。
     */
    private static final int MAX_RESULT_LENGTH = 500;
    /**
     * 摘要式改写模板。
     */
    private static final String ABSTRACTIVE_TEMPLATE = "rag/query-rewrite-abstractive.v1";
    /**
     * 抽取式改写模板。
     */
    private static final String EXTRACTIVE_TEMPLATE = "rag/query-rewrite-extractive.v1";
    /**
     * 混合改写模板。
     */
    private static final String HYBRID_TEMPLATE = "rag/query-rewrite-hybrid.v1";
    /**
     * 多轮历史改写模板。
     */
    private static final String HISTORY_TEMPLATE = "rag/query-rewrite-with-history.v1";

    private final DynamicModelManager dynamicModelManager;
    private final PromptTemplateManager promptTemplateManager;

    /**
     * 构造查询改写服务。
     */
    public QueryRewriteService(DynamicModelManager dynamicModelManager,
                               PromptTemplateManager promptTemplateManager) {
        this.dynamicModelManager = dynamicModelManager;
        this.promptTemplateManager = promptTemplateManager;
    }

    /**
     * 默认查询改写入口。
     */
    @Override
    public String rewrite(String originalQuery) {
        if (originalQuery == null || originalQuery.trim().length() <= MIN_QUERY_LENGTH) {
            log.debug("查询过短，跳过改写");
            return originalQuery;
        }
        return rewriteAbstractive(originalQuery.trim());
    }

    /**
     * 结合会话历史和用户记忆改写查询。
     */
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

    /**
     * 摘要式改写。
     */
    @Override
    public String rewriteAbstractive(String originalQuery) {
        return doRewrite(originalQuery, ABSTRACTIVE_TEMPLATE, "abstractive");
    }

    /**
     * 抽取式改写。
     */
    @Override
    public String rewriteExtractive(String originalQuery) {
        return doRewrite(originalQuery, EXTRACTIVE_TEMPLATE, "extractive");
    }

    /**
     * 混合式改写。
     */
    @Override
    public String rewriteHybrid(String originalQuery) {
        return doRewrite(originalQuery, HYBRID_TEMPLATE, "hybrid");
    }

    /**
     * 使用指定模板执行改写。
     */
    private String doRewrite(String originalQuery, String templatePath, String strategyName) {
        return doRewriteWithMessages(originalQuery, strategyName, List.of(
                promptTemplateManager.renderSystem(templatePath, null),
                UserMessage.from(originalQuery)
        ));
    }

    /**
     * 调用模型执行改写，失败时回退原始查询。
     */
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
