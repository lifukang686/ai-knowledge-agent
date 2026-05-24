package com.fukang.knowledge.agent.rag;

import com.fukang.knowledge.agent.api.qa.dto.QaResp;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.enums.ModelTypeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.infrastructure.ai.DynamicModelManager;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.KnowledgeBaseMapper;
import com.fukang.knowledge.agent.rag.chain.RerankService;
import com.fukang.knowledge.agent.rag.config.RetrievalProperties;
import com.fukang.knowledge.agent.rag.model.SearchResult;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * RAG 问答编排服务
 * <p>统一采用"检索 → 多因子重排序 → LLM 生成回答"的三阶段流程。
 * 支持 knowledgeBaseId 为空时的全量检索场景，检索失败时自动降级</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagAppService {

    private static final String SYSTEM_PROMPT =
            "你是一个专业的知识库问答助手。请严格基于提供的文档内容回答问题，不要编造信息。";

    private static final String NOT_FOUND_MESSAGE = "抱歉，未找到与您问题相关的文档内容。";

    private final QueryRewriteService queryRewriteService;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final SemanticSearchService semanticSearchService;
    private final RetrievalProperties retrievalProperties;
    private final RerankService rerankService;
    private final DynamicModelManager dynamicModelManager;

    /**
     * RAG 问答核心流程
     *
     * @param question        用户自然语言问题
     * @param knowledgeBaseId 目标知识库 ID（null 表示全量检索）
     * @param conversationId  会话 ID（预留多轮对话）
     * @return 问答响应
     * @throws BaseException knowledgeBaseId 非 null 但对应知识库不存在时抛出
     */
    public QaResp answer(String question, Long knowledgeBaseId, Long conversationId) {
        if (knowledgeBaseId != null && knowledgeBaseMapper.selectById(knowledgeBaseId) == null) {
            log.warn("知识库不存在: id={}", knowledgeBaseId);
            throw new BaseException(ErrorCodeEnum.KNOWLEDGE_BASE_NOT_EXIST);
        }

        String rewrittenQuery = queryRewriteService.rewrite(question);
        double threshold = retrievalProperties.getSimilarityThreshold();
        int topK = retrievalProperties.getTopK();

        List<SearchResult> results = retrieveWithFallback(rewrittenQuery, question, knowledgeBaseId, topK, threshold);

        results = rerankService.rerank(results, question);

        String answer = generateAnswer(results, rewrittenQuery);
        String status = results.isEmpty() ? "no_results" : "success";
        return new QaResp(answer, rewrittenQuery, status);
    }

    /**
     * 双路检索：改写查询优先，原始查询补充
     */
    private List<SearchResult> retrieveWithFallback(String rewrittenQuery, String originalQuery,
                                                     Long knowledgeBaseId, int topK, double threshold) {
        List<SearchResult> rewrittenResults = semanticSearchService.searchWithPgVector(
                rewrittenQuery, knowledgeBaseId, topK, threshold);

        if (rewrittenResults.size() >= topK || rewrittenQuery.equals(originalQuery)) {
            return rewrittenResults;
        }

        log.info("改写查询检索结果不足 ({} < {})，使用原始查询补充检索", rewrittenResults.size(), topK);
        List<SearchResult> originalResults = semanticSearchService.searchWithPgVector(
                originalQuery, knowledgeBaseId, topK, threshold);

        Set<Long> existingChunkIds = rewrittenResults.stream()
                .map(SearchResult::chunkId)
                .collect(Collectors.toSet());

        List<SearchResult> allResults = new ArrayList<>(rewrittenResults);
        for (SearchResult r : originalResults) {
            if (!existingChunkIds.contains(r.chunkId())) {
                allResults.add(r);
                existingChunkIds.add(r.chunkId());
            }
        }
        allResults.sort(Comparator.comparingDouble(SearchResult::similarity).reversed());
        if (allResults.size() > topK) {
            allResults = allResults.subList(0, topK);
        }
        return allResults;
    }

    /**
     * 通过 LLM 基于检索结果生成回答
     */
    private String generateAnswer(List<SearchResult> results, String query) {
        if (results.isEmpty()) {
            return NOT_FOUND_MESSAGE;
        }

        StringBuilder context = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            SearchResult r = results.get(i);
            context.append("【文档片段").append(i + 1).append("】").append(r.chunkText()).append("\n");
        }

        String userPrompt = String.format(
                "请基于以下文档内容回答问题：\n\n%s\n\n问题：%s", context, query);

        try {
            ChatLanguageModel chatModel = dynamicModelManager.getChatModel(ModelTypeEnum.CHAT);
            List<ChatMessage> messages = List.of(
                    SystemMessage.from(SYSTEM_PROMPT),
                    UserMessage.from(userPrompt)
            );
            Response<AiMessage> response = chatModel.generate(messages);
            String answer = response.content().text();
            if (answer == null || answer.isBlank()) {
                log.warn("LLM 返回空回答");
                return NOT_FOUND_MESSAGE;
            }
            return answer;
        } catch (Exception e) {
            log.error("LLM 生成回答失败，降级为文本拼接", e);
            return formatFallbackAnswer(results);
        }
    }

    /**
     * 降级回答：直接拼接检索结果
     */
    private String formatFallbackAnswer(List<SearchResult> results) {
        if (results.isEmpty()) {
            return NOT_FOUND_MESSAGE;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("为您找到以下相关内容：\n\n");
        for (int i = 0; i < results.size(); i++) {
            SearchResult r = results.get(i);
            sb.append("【").append(i + 1).append("】").append(r.chunkText()).append("\n");
            sb.append("（相关度：").append(String.format("%.2f", r.similarity())).append("）\n\n");
        }
        return sb.toString().trim();
    }
}