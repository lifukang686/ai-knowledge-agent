package com.fukang.knowledge.agent.application.rag;

import com.fukang.knowledge.agent.application.ai.port.ChatCompletionPort;
import com.fukang.knowledge.agent.application.rag.result.QaResult;
import com.fukang.knowledge.agent.application.knowledge.port.KnowledgeBaseRepository;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.domain.rag.model.SearchResult;
import com.fukang.knowledge.agent.domain.rag.service.AnswerGenerator;
import com.fukang.knowledge.agent.domain.rag.service.QueryRewritePort;
import com.fukang.knowledge.agent.domain.rag.service.Reranker;
import com.fukang.knowledge.agent.domain.rag.service.RetrievalStrategy;
import com.fukang.knowledge.agent.infrastructure.ai.PromptTemplateManager;
import com.fukang.knowledge.agent.infrastructure.config.RetrievalProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagAppService {

    private static final String CHITCHAT_SYSTEM_TEMPLATE = "rag/chitchat-system.v1";

    private static final Pattern CHITCHAT_PATTERN = Pattern.compile(
            "(你好|您好|嗨|哈喽|hello|hi|早上好|下午好|晚上好|晚安|再见|拜拜|谢谢|感谢|辛苦了"
                    + "|(你是谁|你叫什么|你能|你可以|你会|你擅长|你有什么|功能|能力|本事)"
                    + "|自我介绍|介绍一下|做个介绍"
                    + "|(讲个|说个|来个)?(笑话|故事)"
                    + "|(天气|几点了|今天(几号|星期几|周几)|现在几点|时间)"
                    + "|(帮我)?(翻译|算一下|计算))",
            Pattern.CASE_INSENSITIVE
    );

    private final QueryRewritePort queryRewritePort;
    private final RetrievalStrategy retrievalStrategy;
    private final Reranker reranker;
    private final AnswerGenerator answerGenerator;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final RetrievalProperties retrievalProperties;
    private final PromptTemplateManager promptTemplateManager;
    private final ChatCompletionPort chatCompletionPort;

    public QaResult answer(String question, Long knowledgeBaseId, Long conversationId) {
        validateKnowledgeBase(knowledgeBaseId);

        if (isChitchat(question)) {
            log.info("检测到非知识库提问，直接使用 LLM 回答: {}", question);
            return new QaResult(directChat(question), question, "success");
        }

        String rewrittenQuery = queryRewritePort.rewrite(question);
        List<SearchResult> retrieved = retrieveWithFallback(rewrittenQuery, question, knowledgeBaseId);

        if (retrieved.isEmpty() && isChitchat(rewrittenQuery)) {
            log.info("检索无结果且改写后为闲聊类问题，降级为直接 LLM 回答");
            return new QaResult(directChat(rewrittenQuery), rewrittenQuery, "success");
        }

        List<SearchResult> reranked = reranker.rerank(retrieved, question);
        String answer = answerGenerator.generateAnswer(reranked, rewrittenQuery);
        log.info("回答内容" + answer);
        String status = reranked.isEmpty() ? "no_results" : "success";
        return new QaResult(answer, rewrittenQuery, status);
    }

    private void validateKnowledgeBase(Long knowledgeBaseId) {
        if (knowledgeBaseId != null && knowledgeBaseRepository.findById(knowledgeBaseId) == null) {
            log.warn("知识库不存在: id={}", knowledgeBaseId);
            throw new BaseException(ErrorCodeEnum.KNOWLEDGE_BASE_NOT_EXIST);
        }
    }

    private boolean isChitchat(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return text.trim().length() <= 20 && CHITCHAT_PATTERN.matcher(text).find();
    }

    private String directChat(String question) {
        try {
            String answer = chatCompletionPort.complete(List.of(
                    ChatCompletionPort.Message.system(promptTemplateManager.renderText(CHITCHAT_SYSTEM_TEMPLATE, null)),
                    ChatCompletionPort.Message.user(question)
            ));
            return answer == null || answer.isBlank()
                    ? "你好！有什么可以帮您的吗？"
                    : answer;
        } catch (Exception e) {
            log.error("直接 LLM 回答失败", e);
            return "你好！我是智能问答助手，有什么可以帮您的吗？";
        }
    }

    private List<SearchResult> retrieveWithFallback(String rewrittenQuery, String originalQuery, Long knowledgeBaseId) {
        int topK = retrievalProperties.getTopK();
        double threshold = retrievalProperties.getSimilarityThreshold();

        List<SearchResult> rewrittenResults = retrievalStrategy.retrieve(
                rewrittenQuery, knowledgeBaseId, topK, threshold);

        if (rewrittenResults.size() >= topK || rewrittenQuery.equals(originalQuery)) {
            return rewrittenResults;
        }

        log.info("改写查询检索结果不足({} < {})，使用原始查询补充检索", rewrittenResults.size(), topK);
        List<SearchResult> originalResults = retrievalStrategy.retrieve(
                originalQuery, knowledgeBaseId, topK, threshold);

        Set<Long> existingChunkIds = rewrittenResults.stream()
                .map(SearchResult::chunkId)
                .collect(Collectors.toSet());

        List<SearchResult> allResults = new ArrayList<>(rewrittenResults);
        for (SearchResult result : originalResults) {
            if (!existingChunkIds.contains(result.chunkId())) {
                allResults.add(result);
                existingChunkIds.add(result.chunkId());
            }
        }
        allResults.sort(Comparator.comparingDouble(SearchResult::similarity).reversed());
        if (allResults.size() > topK) {
            allResults = allResults.subList(0, topK);
        }
        return allResults;
    }
}
