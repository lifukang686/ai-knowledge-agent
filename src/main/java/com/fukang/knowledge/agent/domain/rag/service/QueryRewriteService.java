package com.fukang.knowledge.agent.domain.rag.service;

import com.fukang.knowledge.agent.common.enums.ModelTypeEnum;
import com.fukang.knowledge.agent.infrastructure.ai.DynamicModelManager;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
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
public class QueryRewriteService {

    private static final int MIN_QUERY_LENGTH = 3;
    private static final int MAX_RESULT_LENGTH = 500;

    private static final String ABSTRACTIVE_SYSTEM_PROMPT =
            "你是一个查询改写专家。你需要将简短的用户查询扩展为更完整、更具体的检索查询。" +
            "添加相关的同义词、上下文和关键概念。只返回改写后的查询文本，不要添加任何解释。";

    private static final String EXTRACTIVE_SYSTEM_PROMPT =
            "你是一个关键词提取专家。从用户查询中提取3-5个最核心的关键词，用空格分隔。" +
            "只返回关键词，不要添加任何解释。";

    private static final String HYBRID_SYSTEM_PROMPT =
            "你是一个查询改写专家。首先将用户查询扩展为更完整的检索查询，然后提取3-5个核心关键词。" +
            "返回格式：扩展查询文本 | 关键词1 关键词2 关键词3";

    private final DynamicModelManager dynamicModelManager;

    public QueryRewriteService(DynamicModelManager dynamicModelManager) {
        this.dynamicModelManager = dynamicModelManager;
    }

    /**
     * 改写查询（默认策略：abstractive）
     *
     * <p>对用户原始查询进行改写，生成更适合向量语义检索的扩展查询文本。
     * 短查询直接返回原查询，LLM 调用异常时降级返回原查询。</p>
     *
     * @param originalQuery 原始用户查询
     * @return 改写后的查询文本，短查询或改写失败时返回原始查询
     */
    public String rewrite(String originalQuery) {
        if (originalQuery == null || originalQuery.trim().length() <= MIN_QUERY_LENGTH) {
            log.debug("查询过短（≤{}字符），跳过改写", MIN_QUERY_LENGTH);
            return originalQuery;
        }
        return rewriteAbstractive(originalQuery.trim());
    }

    /**
     * 抽象扩展改写策略
     *
     * <p>调用 LLM 将简短的用户查询扩展为语义更丰富的检索查询，
     * 添加相关的同义词、上下文和关键概念。</p>
     *
     * @param originalQuery 原始用户查询
     * @return 改写后的扩展查询文本，异常时返回原始查询
     */
    public String rewriteAbstractive(String originalQuery) {
        return doRewrite(originalQuery, ABSTRACTIVE_SYSTEM_PROMPT, "abstractive");
    }

    /**
     * 关键词提取改写策略
     *
     * <p>从原始查询中调用 LLM 提取 3-5 个核心关键词，用空格连接。
     * 不做额外的语义扩展，只保留关键术语。</p>
     *
     * @param originalQuery 原始用户查询
     * @return 空格连接的关键词文本，异常时返回原始查询
     */
    public String rewriteExtractive(String originalQuery) {
        return doRewrite(originalQuery, EXTRACTIVE_SYSTEM_PROMPT, "extractive");
    }

    /**
     * 混合改写策略
     *
     * <p>同时执行抽象扩展和关键词提取，返回格式为
     * "扩展查询文本 | 关键词1 关键词2 关键词3"。</p>
     *
     * @param originalQuery 原始用户查询
     * @return 扩展查询和关键词的混合文本，异常时返回原始查询
     */
    public String rewriteHybrid(String originalQuery) {
        return doRewrite(originalQuery, HYBRID_SYSTEM_PROMPT, "hybrid");
    }

    /**
     * 核心改写逻辑
     *
     * <p>通过 DynamicModelManager 获取 ChatModel，适配为 langchain4j
     * ChatLanguageModel 后发起 LLM 调用。</p>
     *
     * @param originalQuery 原始用户查询
     * @param systemPrompt  系统提示词（策略相关）
     * @param strategyName  策略名称（用于日志）
     * @return 改写后的查询文本，异常时返回原始查询
     */
    private String doRewrite(String originalQuery, String systemPrompt, String strategyName) {
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
                    SystemMessage.from(systemPrompt),
                    UserMessage.from(originalQuery)
            );

            log.debug("改写前原文: {}", originalQuery);
            Response<AiMessage> response = chatModel.generate(messages);
            String rewritten = response.content().text();

            if (rewritten == null || rewritten.isBlank()) {
                log.warn("LLM 返回空结果，回退使用原始查询: strategy={}", strategyName);
                return originalQuery;
            }

            if (rewritten.length() > MAX_RESULT_LENGTH) {
                rewritten = rewritten.substring(0, MAX_RESULT_LENGTH);
            }

            long elapsed = System.currentTimeMillis() - start;
            log.info("查询改写完成: strategy={}, 耗时={}ms", strategyName, elapsed);
            log.debug("改写后文本: {}", rewritten);
            return rewritten;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.warn("查询改写失败，回退使用原始查询: strategy={}, 耗时={}ms, error={}",
                    strategyName, elapsed, e.getMessage());
            return originalQuery;
        }
    }
}