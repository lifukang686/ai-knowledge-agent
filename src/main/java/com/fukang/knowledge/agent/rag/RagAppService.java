package com.fukang.knowledge.agent.rag;

import com.fukang.knowledge.agent.api.qa.dto.QaResp;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.KnowledgeBaseMapper;
import com.fukang.knowledge.agent.rag.chain.RagChainBuilder;
import com.fukang.knowledge.agent.rag.chain.RerankService;
import com.fukang.knowledge.agent.rag.config.RetrievalProperties;
import com.fukang.knowledge.agent.rag.model.SearchResult;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
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
 * <p>负责 RAG 问答流程的完整编排，集成语义检索和 langchain4j 链式 RAG 能力。
 * 优先使用 langchain4j 的 ContentRetriever + RetrievalAugmentor + AiServices 链式编排，
 * 失败时自动降级为手动双路检索编排以保证可用性</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagAppService {

    private final QueryRewriteService queryRewriteService;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final SemanticSearchService semanticSearchService;
    private final RetrievalProperties retrievalProperties;
    private final RagChainBuilder ragChainBuilder;
    private final RerankService rerankService;

    /**
     * RAG 问答核心流程
     * <p>使用 langchain4j 链式 RAG 编排：查询改写 → ContentRetriever 检索 →
     * RetrievalAugmentor 增强 → AiServices 生成回答。
     * 链式编排异常时自动降级为手动双路检索编排</p>
     *
     * @param question        用户自然语言问题
     * @param knowledgeBaseId 目标知识库 ID
     * @param conversationId  会话 ID（可选，预留多轮对话）
     * @return 问答响应，包含回答文本和改写后的查询
     * @throws BaseException 知识库不存在时抛出 KNOWLEDGE_BASE_NOT_EXIST
     */
    public QaResp answer(String question, Long knowledgeBaseId, Long conversationId) {
        if (knowledgeBaseMapper.selectById(knowledgeBaseId) == null) {
            log.warn("知识库不存在: id={}", knowledgeBaseId);
            throw new BaseException(ErrorCodeEnum.KNOWLEDGE_BASE_NOT_EXIST);
        }

        String rewrittenQuery = queryRewriteService.rewrite(question);
        double threshold = retrievalProperties.getSimilarityThreshold();

        try {
            ContentRetriever contentRetriever = ragChainBuilder.buildContentRetriever(knowledgeBaseId);
            RetrievalAugmentor augmentor = rerankService.buildReRankingAugmentor(contentRetriever, threshold);

            ChatLanguageModel chatModel = ragChainBuilder.createChatModel();
            RagAssistant assistant = AiServices.builder(RagAssistant.class)
                    .chatLanguageModel(chatModel)
                    .retrievalAugmentor(augmentor)
                    .build();

            String answer = assistant.answer(rewrittenQuery);
            return new QaResp(answer, rewrittenQuery, "success");
        } catch (Exception e) {
            log.error("langchain4j RAG 链执行异常，降级为手动编排", e);
            return answerWithFallback(question, knowledgeBaseId, rewrittenQuery);
        }
    }

    /**
     * 手动编排降级方案
     * <p>当 langchain4j 链执行失败时，回退到原有手动编排逻辑：
     * 双路检索（改写查询优先，原始查询补充）→ 结果去重排序 → 格式化输出</p>
     *
     * @param question        原始用户问题
     * @param knowledgeBaseId 目标知识库 ID
     * @param rewrittenQuery  改写后的查询
     * @return 问答响应
     */
    private QaResp answerWithFallback(String question, Long knowledgeBaseId, String rewrittenQuery) {
        int topK = retrievalProperties.getTopK();
        double threshold = retrievalProperties.getSimilarityThreshold();

        List<SearchResult> rewrittenResults = semanticSearchService.searchWithPgVector(
                rewrittenQuery, knowledgeBaseId, topK, threshold);

        List<SearchResult> allResults = new ArrayList<>(rewrittenResults);
        if (rewrittenResults.size() < topK && !rewrittenQuery.equals(question)) {
            log.info("改写查询检索结果不足 ({} < {})，使用原始查询补充检索", rewrittenResults.size(), topK);
            List<SearchResult> originalResults = semanticSearchService.searchWithPgVector(
                    question, knowledgeBaseId, topK, threshold);
            Set<Long> existingChunkIds = rewrittenResults.stream()
                    .map(SearchResult::chunkId)
                    .collect(Collectors.toSet());
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
        }

        String answer;
        if (allResults.isEmpty()) {
            answer = "抱歉，未找到与您问题相关的文档内容。";
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("为您找到以下相关内容：\n\n");
            for (int i = 0; i < allResults.size(); i++) {
                SearchResult r = allResults.get(i);
                sb.append("【").append(i + 1).append("】").append(r.chunkText()).append("\n");
                sb.append("（相似度：").append(String.format("%.2f", r.similarity())).append("）\n\n");
            }
            answer = sb.toString().trim();
        }

        return new QaResp(answer, rewrittenQuery, "success");
    }

    /**
     * RAG 问答助手接口
     * <p>langchain4j AiServices 代理接口，由 AiServices 动态生成实现类，
     * 自动集成 RetrievalAugmentor 进行检索增强</p>
     */
    interface RagAssistant {

        @SystemMessage("你是一个专业的知识库问答助手。请严格基于提供的文档内容回答问题，不要编造信息。")
        @UserMessage("{{it}}")
        String answer(String question);
    }
}