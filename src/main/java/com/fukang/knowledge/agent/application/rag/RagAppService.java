package com.fukang.knowledge.agent.application.rag;

import com.fukang.knowledge.agent.api.qa.dto.QaResp;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.enums.ModelTypeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.domain.rag.model.SearchResult;
import com.fukang.knowledge.agent.infrastructure.rag.QueryRewriteService;
import com.fukang.knowledge.agent.infrastructure.rag.HybridSearchService;
import com.fukang.knowledge.agent.infrastructure.ai.DynamicModelManager;
import com.fukang.knowledge.agent.infrastructure.config.RetrievalProperties;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.KnowledgeBaseMapper;
import com.fukang.knowledge.agent.infrastructure.rag.RerankService;
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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * RAG 问答编排服务
 * <p>采用意图检测 + "检索 → 多因子重排序 → LLM 生成回答"的三阶段流程。
 * 寒暄及非知识库类问题直接走 LLM，跳过检索；知识类问题走完整管线。
 * 支持 knowledgeBaseId 为空时的全量检索场景</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagAppService {

    private static final String SYSTEM_PROMPT =
            "你是一个专业的知识库问答助手，必须确保每条回答都能在提供的文档中找到依据。\n" +
                    "任务：根据用户消息中提供的文档内容，回答用户的问题。\n" +
                    "格式要求：\n" +
                    "- 先给出直接答案（一句话）。\n" +
                    "- 然后逐条列出支撑依据，每条依据后注明所在的文档片段编号（如“文档片段1”）。\n" +
                    "- 如果答案涉及多个事实，用分点列出，每条后面用括号标注出处。\n\n" +
                    "约束：\n" +
                    "1. 如果文档未提供足够信息回答，请回复：“文档中未找到关于[具体问题]的信息。” 不要补充外部知识。\n" +
                    "2. 禁止改写文档原意。\n" +
                    "3. 不要添加“根据我的知识”等主观表述。";

    private static final String NOT_FOUND_MESSAGE = "抱歉，未找到与您问题相关的文档内容。";

    /**
     * 非知识库提问的正则模式
     * <p>匹配寒暄、自称、能力询问等无需检索知识库即可回答的对话</p>
     */
    private static final Pattern CHITCHAT_PATTERN = Pattern.compile(
            "(你好|您好|嗨|哈[喽啰]|早上好|下午好|晚上好|晚安|再见|拜拜|谢谢|感谢|辛苦了)"
            + "|(你(是|叫|能|可以|会|有)什么)|(谁|哪位|叫什么名字)"
            + "|(你能|你可以|你会|你擅长|你有什么(功能|能力|本事))"
            + "|自我介绍|做个介绍|介绍(一下|下)自己"
            + "|(讲个|说个|来个)?(笑话|故事)"
            + "|(天气|几点了|今天(几号|星期几|周几)|现在几(点|时))"
            + "|(帮我)?(翻译|算(一下|下)|计算)"
    );

    private static final String CHITCHAT_SYSTEM_PROMPT =
            "你是一个友好的AI助手。请用自然、亲切的语气回答用户的问题，不需要引用任何文档。";

    private final QueryRewriteService queryRewriteService;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final HybridSearchService hybridSearchService;
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

        if (isChitchat(question)) {
            log.info("检测到非知识库提问（寒暄/能力询问），直接 LLM 回答: {}", question);
            String answer = directChat(question);
            return new QaResp(answer, question, "success");
        }

        String rewrittenQuery = queryRewriteService.rewrite(question);
        double threshold = retrievalProperties.getSimilarityThreshold();
        int topK = retrievalProperties.getTopK();

        List<SearchResult> results = retrieveWithFallback(rewrittenQuery, question, knowledgeBaseId, topK, threshold);

        if (results.isEmpty() && isChitchat(rewrittenQuery)) {
            log.info("检索无结果且改写后为寒暄类问题，降级为直接 LLM 回答");
            String answer = directChat(rewrittenQuery);
            return new QaResp(answer, rewrittenQuery, "success");
        }

        results = rerankService.rerank(results, question);

        String answer = generateAnswer(results, rewrittenQuery);
        String status = results.isEmpty() ? "no_results" : "success";
        return new QaResp(answer, rewrittenQuery, status);
    }

    /**
     * 检测是否为非知识库类提问
     */
    private boolean isChitchat(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return text.trim().length() <= 20 && CHITCHAT_PATTERN.matcher(text).find();
    }

    /**
     * 直接 LLM 回答（跳过检索）
     */
    private String directChat(String question) {
        try {
            ChatLanguageModel chatModel = dynamicModelManager.getChatModel(ModelTypeEnum.CHAT);
            List<ChatMessage> messages = List.of(
                    SystemMessage.from(CHITCHAT_SYSTEM_PROMPT),
                    UserMessage.from(question)
            );
            Response<AiMessage> response = chatModel.generate(messages);
            String answer = response.content().text();
            if (answer == null || answer.isBlank()) {
                return "你好！有什么可以帮你的吗？";
            }
            return answer;
        } catch (Exception e) {
            log.error("直接 LLM 回答失败", e);
            return "你好！我是智能问答助手，有什么可以帮你的吗？";
        }
    }

    /**
     * 混合检索：改写查询优先，原始查询补充
     */
    private List<SearchResult> retrieveWithFallback(String rewrittenQuery, String originalQuery,
                                                     Long knowledgeBaseId, int topK, double threshold) {
        List<SearchResult> rewrittenResults = hybridSearchService.search(
                rewrittenQuery, knowledgeBaseId, topK, threshold);

        if (rewrittenResults.size() >= topK || rewrittenQuery.equals(originalQuery)) {
            return rewrittenResults;
        }

        log.info("改写查询检索结果不足 ({} < {})，使用原始查询补充检索", rewrittenResults.size(), topK);
        List<SearchResult> originalResults = hybridSearchService.search(
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

        // 用户消息包含文档内容和问题
        String userPrompt = String.format(
                "【文档内容】\n%s\n\n【用户问题】\n%s", context.toString(), query);

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