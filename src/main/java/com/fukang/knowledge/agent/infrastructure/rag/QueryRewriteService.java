package com.fukang.knowledge.agent.infrastructure.rag;

import com.fukang.knowledge.agent.common.enums.ModelTypeEnum;
import com.fukang.knowledge.agent.domain.rag.service.QueryRewritePort;
import com.fukang.knowledge.agent.infrastructure.ai.DynamicModelManager;
import com.fukang.knowledge.agent.infrastructure.ai.PromptTemplateManager;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * RAG 查询改写服务（多策略版本）
 *
 * <p>基于 langchain4j 实现三种查询改写策略，通过 DynamicModelManager 动态获取
 * Spring AI ChatModel 实例，经 SpringAiChatModelAdapter 适配为 langchain4j
 * ChatLanguageModel 进行 LLM 调用。所有提示词硬编码在代码中，不依赖任何外部模板文件。</p>
 *
 * <h3>支持策略</h3>
 * <ul>
 *   <li><b>abstractive（抽象扩展改写）</b>：调用 LLM 生成语义丰富的扩展查询，添加同义词、上下文和关键概念</li>
 *   <li><b>extractive（关键词提取改写）</b>：从原始查询中提取 3-5 个核心关键词，用空格连接</li>
 *   <li><b>hybrid（混合改写）</b>：先做抽象扩展，同时输出关键词版本，用 " | " 分隔</li>
 * </ul>
 *
 * <p>默认使用 abstractive 策略。短查询（≤3 字符）和 LLM 调用异常均有降级策略，
 * 保证检索流程的鲁棒性。</p>
 *
 * @author fukang
 */
@Slf4j
@Service
public class QueryRewriteService implements QueryRewritePort {

    private static final int MIN_QUERY_LENGTH = 3;
    private static final int MAX_RESULT_LENGTH = 500;
    private static final String ABSTRACTIVE_TEMPLATE = "rag/query-rewrite-abstractive.v1";
    private static final String EXTRACTIVE_TEMPLATE = "rag/query-rewrite-extractive.v1";
    private static final String HYBRID_TEMPLATE = "rag/query-rewrite-hybrid.v1";

    private final DynamicModelManager dynamicModelManager;
    private final PromptTemplateManager promptTemplateManager;

    public QueryRewriteService(DynamicModelManager dynamicModelManager,
                               PromptTemplateManager promptTemplateManager) {
        this.dynamicModelManager = dynamicModelManager;
        this.promptTemplateManager = promptTemplateManager;
    }

    public String rewrite(String originalQuery) {
        if (originalQuery == null || originalQuery.trim().length() <= MIN_QUERY_LENGTH) {
            log.debug("查询过短，跳过改写");
            return originalQuery;
        }
        return rewriteAbstractive(originalQuery.trim());
    }

    public String rewriteAbstractive(String originalQuery) {
        return doRewrite(originalQuery, ABSTRACTIVE_TEMPLATE, "abstractive");
    }

    public String rewriteExtractive(String originalQuery) {
        return doRewrite(originalQuery, EXTRACTIVE_TEMPLATE, "extractive");
    }

    public String rewriteHybrid(String originalQuery) {
        return doRewrite(originalQuery, HYBRID_TEMPLATE, "hybrid");
    }

    private String doRewrite(String originalQuery, String templatePath, String strategyName) {
        long start = System.currentTimeMillis();
        log.info("开始查询改写: strategy={}", strategyName);

        ChatLanguageModel chatModel;
        try {
            chatModel = dynamicModelManager.getChatModel(ModelTypeEnum.CHAT);
        } catch (Exception e) {
            log.warn("获取 ChatModel 失败，回退使用原始查询: strategy={}, error={}",
                    strategyName, e.getMessage());
            return originalQuery;
        }

        try {
            List<ChatMessage> messages = List.of(
                    promptTemplateManager.renderSystem(templatePath, null),
                    UserMessage.from(originalQuery)
            );

            Response<AiMessage> response = chatModel.generate(messages);
            String rewritten = response.content().text();

            if (rewritten == null || rewritten.isBlank()) {
                log.warn("LLM 返回空改写结果，回退使用原始查询: strategy={}", strategyName);
                return originalQuery;
            }

            if (rewritten.length() > MAX_RESULT_LENGTH) {
                rewritten = rewritten.substring(0, MAX_RESULT_LENGTH);
            }

            log.info("查询改写完成: strategy={}, elapsedMs={}", strategyName, System.currentTimeMillis() - start);
            log.debug("改写前: {}, 改写后: {}", originalQuery, rewritten);
            return rewritten;
        } catch (Exception e) {
            log.warn("查询改写失败，回退使用原始查询: strategy={}, elapsedMs={}, error={}",
                    strategyName, System.currentTimeMillis() - start, e.getMessage());
            return originalQuery;
        }
    }
}
