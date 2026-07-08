package com.fukang.knowledge.agent.module.conversation.service;

import com.fukang.knowledge.agent.module.modelruntime.service.client.impl.ChatModelClient;
import com.fukang.knowledge.agent.module.modelruntime.model.vo.ChatMessage;
import com.fukang.knowledge.agent.module.conversation.mapper.ConversationMapper;
import com.fukang.knowledge.agent.module.conversation.mapper.ConversationMessageMapper;
import com.fukang.knowledge.agent.module.conversation.mapper.ConversationSummaryMapper;
import com.fukang.knowledge.agent.common.context.UserContextHolder;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.module.modelruntime.service.manager.PromptTemplateManager;
import com.fukang.knowledge.agent.module.conversation.model.entity.ConversationEntity;
import com.fukang.knowledge.agent.module.conversation.model.entity.ConversationMessageEntity;
import com.fukang.knowledge.agent.module.conversation.model.entity.ConversationSummaryEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RAG 会话记忆服务。
 * <p>负责会话创建、短期历史读取、摘要压缩和消息落库。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationMemoryService {

    /** 用户消息角色。 */
    private static final String ROLE_USER = "user";
    /** 助手消息角色。 */
    private static final String ROLE_ASSISTANT = "assistant";
    /** 新建会话的默认状态。 */
    private static final String STATUS_ACTIVE = "active";
    /** 会话摘要 Prompt 模板。 */
    private static final String SUMMARY_TEMPLATE = "rag/conversation-summary.v1";
    /** 查询改写最多参考的最近消息数。 */
    private static final int REWRITE_HISTORY_LIMIT = 6;
    /** 回答生成最多参考的最近消息数。 */
    private static final int ANSWER_HISTORY_LIMIT = 6;
    /** 超过该消息数后触发长历史摘要。 */
    private static final int SUMMARY_TRIGGER_MESSAGE_COUNT = 16;
    /** 摘要时保留在短期记忆中的最近消息数。 */
    private static final int SUMMARY_KEEP_RECENT_COUNT = 6;
    /** 会话标题最大长度。 */
    private static final int TITLE_MAX_LENGTH = 60;

    private final ConversationMapper conversationMapper;
    private final ConversationMessageMapper conversationMessageMapper;
    private final ConversationSummaryMapper conversationSummaryMapper;
    private final ChatModelClient chatCompletionPort;
    private final PromptTemplateManager promptTemplateManager;

    /**
     * 准备本轮问答上下文；conversationId 为空时会自动创建新会话。
     *
     * @param conversationId  会话ID，可为空
     * @param knowledgeBaseId 知识库ID
     * @param question        用户问题
     */
    @Transactional(rollbackFor = Exception.class)
    public ConversationMemoryContext prepareContext(Long conversationId, Long knowledgeBaseId, String question) {
        ConversationEntity conversation = resolveConversation(conversationId, knowledgeBaseId, question);
        ConversationSummaryEntity summary = latestSummary(conversation.getId());
        List<ConversationMessageEntity> recentMessages = recentMessages(conversation.getId(), REWRITE_HISTORY_LIMIT);
        return new ConversationMemoryContext(
                conversation.getId(),
                summary != null ? summary.getSummary() : "",
                formatHistory(recentMessages, REWRITE_HISTORY_LIMIT, false),
                formatHistory(recentMessages, ANSWER_HISTORY_LIMIT, true)
        );
    }

    /**
     * 保存用户消息。
     *
     * @param conversationId 会话ID
     * @param question       用户问题
     * @param rewrittenQuery 改写查询
     * @param status         本轮状态
     */
    @Transactional(rollbackFor = Exception.class)
    public Long saveUserMessage(Long conversationId, String question, String rewrittenQuery, String status) {
        Long messageId = insertMessage(conversationId, ROLE_USER, question, rewrittenQuery, status);
        refreshConversationTitle(conversationId, question);
        return messageId;
    }

    /**
     * 保存助手消息。
     *
     * @param conversationId 会话ID
     * @param answer         助手回答
     * @param status         本轮状态
     */
    @Transactional(rollbackFor = Exception.class)
    public Long saveAssistantMessage(Long conversationId, String answer, String status) {
        Long messageId = insertMessage(conversationId, ROLE_ASSISTANT, answer, null, status);
        touchConversation(conversationId);
        refreshSummaryIfNeeded(conversationId);
        return messageId;
    }

    /**
     * 获取已有会话；不存在时创建新会话。
     *
     * @param conversationId  会话ID，可为空
     * @param knowledgeBaseId 知识库ID
     * @param question        用户问题
     */
    private ConversationEntity resolveConversation(Long conversationId, Long knowledgeBaseId, String question) {
        if (conversationId != null) {
            ConversationEntity existing = conversationMapper.selectById(conversationId);
            if (existing != null) {
                ensureCurrentUserConversation(existing);
                return existing;
            }
            log.warn("会话不存在，创建新会话替代: conversationId={}", conversationId);
        }

        ConversationEntity conversation = new ConversationEntity();
        conversation.setUserId(currentUserId());
        conversation.setKnowledgeBaseId(knowledgeBaseId);
        conversation.setTitle(shortTitle(question));
        conversation.setStatus(STATUS_ACTIVE);
        conversationMapper.insert(conversation);
        log.info("创建RAG会话: conversationId={}, userId={}", conversation.getId(), conversation.getUserId());
        return conversation;
    }

    /**
     * 防止跨用户复用会话记忆。
     *
     * @param conversation 会话实体
     */
    private void ensureCurrentUserConversation(ConversationEntity conversation) {
        Long userId = currentUserId();
        if (conversation.getUserId() != null && !conversation.getUserId().equals(userId)) {
            throw new BaseException(403, "无权访问该会话");
        }
    }

    /**
     * 写入一条会话消息。
     *
     * @param conversationId 会话ID
     * @param role           消息角色
     * @param content        消息内容
     * @param rewrittenQuery 改写查询
     * @param status         本轮状态
     */
    private Long insertMessage(Long conversationId, String role, String content, String rewrittenQuery, String status) {
        if (conversationId == null || !StringUtils.hasText(content)) {
            return null;
        }
        ConversationMessageEntity message = new ConversationMessageEntity();
        message.setConversationId(conversationId);
        message.setRole(role);
        message.setContent(content);
        message.setRewrittenQuery(rewrittenQuery);
        message.setStatus(status);
        conversationMessageMapper.insert(message);
        return message.getId();
    }

    /**
     * 用首问刷新默认标题。
     *
     * @param conversationId 会话ID
     * @param question       用户问题
     */
    private void refreshConversationTitle(Long conversationId, String question) {
        ConversationEntity conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            return;
        }
        if (StringUtils.hasText(conversation.getTitle()) && !"新会话".equals(conversation.getTitle())) {
            return;
        }
        conversation.setTitle(shortTitle(question));
        conversationMapper.updateById(conversation);
    }

    /**
     * 推进会话更新时间，用于历史列表排序。
     *
     * @param conversationId 会话ID
     */
    private void touchConversation(Long conversationId) {
        ConversationEntity conversation = conversationMapper.selectById(conversationId);
        if (conversation != null) {
            conversationMapper.updateById(conversation);
        }
    }

    /**
     * 查询最近消息，供短期记忆使用。
     *
     * @param conversationId 会话ID
     * @param limit          查询条数
     */
    private List<ConversationMessageEntity> recentMessages(Long conversationId, int limit) {
        return conversationMessageMapper.findRecentByConversationId(conversationId, limit);
    }

    /**
     * 查询最新的长期摘要。
     *
     * @param conversationId 会话ID
     */
    private ConversationSummaryEntity latestSummary(Long conversationId) {
        return conversationSummaryMapper.findLatestByConversationId(conversationId);
    }

    /**
     * 消息过多时压缩旧消息为摘要。
     *
     * @param conversationId 会话ID
     */
    private void refreshSummaryIfNeeded(Long conversationId) {
        List<ConversationMessageEntity> allMessages = allMessages(conversationId);
        if (allMessages.size() <= SUMMARY_TRIGGER_MESSAGE_COUNT) {
            return;
        }

        int summarizeEnd = Math.max(0, allMessages.size() - SUMMARY_KEEP_RECENT_COUNT);
        List<ConversationMessageEntity> messagesToSummarize = allMessages.subList(0, summarizeEnd);
        if (messagesToSummarize.isEmpty()) {
            return;
        }

        ConversationSummaryEntity existing = latestSummary(conversationId);
        Long alreadySummarizedId = existing != null ? existing.getMessageUntilId() : null;
        List<ConversationMessageEntity> unsummarizedMessages = messagesToSummarize.stream()
                .filter(message -> alreadySummarizedId == null
                        || message.getId() == null
                        || message.getId() > alreadySummarizedId)
                .toList();
        if (unsummarizedMessages.isEmpty()) {
            return;
        }

        Long lastMessageId = unsummarizedMessages.get(unsummarizedMessages.size() - 1).getId();
        String history = formatHistory(unsummarizedMessages, unsummarizedMessages.size(), true);
        String oldSummary = existing != null ? existing.getSummary() : "";
        // 只把未压缩过的历史交给模型，避免旧摘要和旧消息重复进入新摘要。
        String summary = generateSummary(oldSummary, history);
        if (!StringUtils.hasText(summary)) {
            return;
        }

        ConversationSummaryEntity summaryDO = existing != null ? existing : new ConversationSummaryEntity();
        summaryDO.setConversationId(conversationId);
        summaryDO.setSummary(summary);
        summaryDO.setMessageUntilId(lastMessageId);
        summaryDO.setTokenEstimate(estimateTokens(summary));
        if (existing == null) {
            conversationSummaryMapper.insert(summaryDO);
        } else {
            conversationSummaryMapper.updateById(summaryDO);
        }
    }

    /**
     * 查询会话全部消息。
     *
     * @param conversationId 会话ID
     */
    private List<ConversationMessageEntity> allMessages(Long conversationId) {
        return conversationMessageMapper.findAllByConversationId(conversationId);
    }

    /**
     * 调用模型生成新的会话摘要。
     *
     * @param oldSummary 已有摘要
     * @param history    待压缩历史
     */
    private String generateSummary(String oldSummary, String history) {
        try {
            String userPrompt = promptTemplateManager.renderText(SUMMARY_TEMPLATE, Map.of(
                    "summary", oldSummary != null ? oldSummary : "",
                    "history", history
            ));
            return chatCompletionPort.complete(List.of(
                    ChatMessage.system("你是会话摘要助手，只保留对后续问答有用的上下文。"),
                    ChatMessage.user(userPrompt)
            ));
        } catch (Exception e) {
            log.warn("会话摘要生成失败，保留旧摘要且不推进摘要覆盖位置: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 将消息列表格式化为 Prompt 历史文本。
     *
     * @param messages         消息列表
     * @param limit            最大条数
     * @param includeAssistant 是否包含助手消息
     */
    private String formatHistory(List<ConversationMessageEntity> messages, int limit, boolean includeAssistant) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }

        int from = Math.max(0, messages.size() - limit);
        List<ConversationMessageEntity> selected = new ArrayList<>(messages.subList(from, messages.size()));
        StringBuilder builder = new StringBuilder();
        for (ConversationMessageEntity message : selected) {
            if (!includeAssistant && ROLE_ASSISTANT.equals(message.getRole())) {
                continue;
            }
            String roleName = ROLE_USER.equals(message.getRole()) ? "用户" : "助手";
            builder.append(roleName).append(": ")
                    .append(message.getContent())
                    .append("\n");
        }
        return builder.toString().trim();
    }

    /**
     * 获取当前用户；RAG 问答链路允许匿名会话，因此可能返回 null。
     */
    private Long currentUserId() {
        return UserContextHolder.getUserId();
    }

    /**
     * 使用首轮问题生成会话标题。
     *
     * @param question 用户问题
     */
    private String shortTitle(String question) {
        if (!StringUtils.hasText(question)) {
            return "新会话";
        }
        String normalized = question.trim().replaceAll("\\s+", " ");
        return normalized.length() > TITLE_MAX_LENGTH
                ? normalized.substring(0, TITLE_MAX_LENGTH)
                : normalized;
    }

    /**
     * 粗略估算摘要 token 数。
     *
     * @param text 摘要文本
     */
    private int estimateTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return Math.max(1, text.length() / 2);
    }
}
