package com.fukang.knowledge.agent.rag;

import com.fukang.knowledge.agent.application.model.AiCallService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * RAG 查询改写服务
 * <p>调用聊天模型对用户原始查询进行改写，生成更适合向量语义检索的扩展查询文本。
 * 短查询（≤3 字符）跳过改写直接返回原查询，模型调用失败时回退使用原始查询，
 * 确保检索流程的鲁棒性</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueryRewriteService {

    private static final int MIN_QUERY_LENGTH = 3;
    private static final int MAX_RESULT_LENGTH = 500;
    private static final String TEMPLATE_PATH = "classpath:prompts/query-rewrite.st";
    private static final String PLACEHOLDER = "{{query}}";

    private final AiCallService aiCallService;
    private final ResourceLoader resourceLoader;

    /**
     * 改写查询
     * <p>加载提示词模板，将原始查询嵌入模板后调用聊天模型生成改写结果。
     * 短查询和模型调用异常均有对应的降级策略</p>
     *
     * @param originalQuery 原始用户查询
     * @return 改写后的查询文本，短查询或改写失败时返回原始查询
     */
    public String rewrite(String originalQuery) {
        if (originalQuery == null || originalQuery.trim().length() <= MIN_QUERY_LENGTH) {
            log.debug("查询过短，跳过改写");
            return originalQuery;
        }

        String template;
        try {
            template = Files.readString(
                    Path.of(resourceLoader.getResource(TEMPLATE_PATH).getURI()),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("查询改写失败，回退使用原始查询", e);
            return originalQuery;
        }

        String prompt = template.replace(PLACEHOLDER, originalQuery);

        String rewritten;
        try {
            rewritten = aiCallService.callModel(prompt);
        } catch (Exception e) {
            log.warn("查询改写失败，回退使用原始查询", e);
            return originalQuery;
        }

        if (rewritten != null && rewritten.length() > MAX_RESULT_LENGTH) {
            rewritten = rewritten.substring(0, MAX_RESULT_LENGTH);
        }

        return rewritten;
    }
}