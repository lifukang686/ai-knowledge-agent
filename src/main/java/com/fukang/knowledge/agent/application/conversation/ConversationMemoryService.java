package com.fukang.knowledge.agent.application.conversation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fukang.knowledge.agent.application.ai.port.ChatCompletionPort;
import com.fukang.knowledge.agent.common.context.UserContextHolder;
import com.fukang.knowledge.agent.infrastructure.ai.PromptTemplateManager;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.ConversationDO;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.ConversationMessageDO;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.ConversationSummaryDO;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.ConversationMapper;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.ConversationMessageMapper;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.ConversationSummaryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
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

    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";
    private static final String STATUS_ACTIVE = "active";
    private static final String SUMMARY_TEMPLATE = "rag/conversation-summary.v1";
    private static final int REWRITE_HISTORY_LIMIT = 6;
    private static final int ANSWER_HISTORY_LIMIT = 6;
    private static final int SUMMARY_TRIGGER_MESSAGE_COUNT = 16;
    private static final int SUMMARY_KEEP_RECENT_COUNT = 6;
    private static final int TITLE_MAX_LENGTH = 60;

    private final ConversationMapper conversationMapper;
    private final ConversationMessageMapper conversationMessageMapper;
    private final ConversationSummaryMapper conversationSummaryMapper;
    private final ChatCompletionPort chatCompletionPort;
    private final PromptTemplateManager promptTemplateManager;

    /**
     * 准备本轮问答上下文；conversationId 为空时会自动创建新会话。
     */
    @Transactional(rollbackFor = Exception.class)
    public ConversationMemoryContext prepareContext(Long conversationId, Long knowledgeBaseId, String question) {
        ConversationDO conversation = resolveConversation(conversationId, knowledgeBaseId, question);
        ConversationSummaryDO summary = latestSummary(conversation.getId());
        List<ConversationMessageDO> recentMessages = recentMessages(conversation.getId(), REWRITE_HISTORY_LIMIT);
        return new ConversationMemoryContext(
                conversation.getId(),
                summary != null ? summary.getSummary() : "",
                recentMessages,
                formatHistory(recentMessages, REWRITE_HISTORY_LIMIT, false),
                formatHistory(recentMessages, ANSWER_HISTORY_LIMIT, true)
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveUserMessage(Long conversationId, String question, String rewrittenQuery, String status) {
        insertMessage(conversationId, ROLE_USER, question, rewrittenQuery, status);
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveAssistantMessage(Long conversationId, String answer, String status) {
        insertMessage(conversationId, ROLE_ASSISTANT, answer, null, status);
        refreshSummaryIfNeeded(conversationId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateConversationTitle(Long conversationId, String question) {
        ConversationDO conversation = conversationMapper.selectById(conversationId);
        if (conversation == null || StringUtils.hasText(conversation.getTitle())) {
            return;
        }
        conversation.setTitle(shortTitle(question));
        conversationMapper.updateById(conversation);
    }

    private ConversationDO resolveConversation(Long conversationId, Long knowledgeBaseId, String question) {
        if (conversationId != null) {
            ConversationDO existing = conversationMapper.selectById(conversationId);
            if (existing != null) {
                return existing;
            }
            log.warn("会话不存在，创建新会话替代: conversationId={}", conversationId);
        }

        ConversationDO conversation = new ConversationDO();
        conversation.setUserId(currentUserId());
        conversation.setKnowledgeBaseId(knowledgeBaseId);
        conversation.setTitle(shortTitle(question));
        conversation.setStatus(STATUS_ACTIVE);
        conversationMapper.insert(conversation);
        return conversation;
    }

    private void insertMessage(Long conversationId, String role, String content, String rewrittenQuery, String status) {
        if (conversationId == null || !StringUtils.hasText(content)) {
            return;
        }
        ConversationMessageDO message = new ConversationMessageDO();
        message.setConversationId(conversationId);
        message.setRole(role);
        message.setContent(content);
        message.setRewrittenQuery(rewrittenQuery);
        message.setStatus(status);
        conversationMessageMapper.insert(message);
    }

    private List<ConversationMessageDO> recentMessages(Long conversationId, int limit) {
        LambdaQueryWrapper<ConversationMessageDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversationMessageDO::getConversationId, conversationId)
                .orderByDesc(ConversationMessageDO::getCreateTime)
                .last("LIMIT " + limit);
        List<ConversationMessageDO> messages = conversationMessageMapper.selectList(wrapper);
        return messages.stream()
                .sorted(Comparator.comparing(ConversationMessageDO::getCreateTime))
                .toList();
    }

    private ConversationSummaryDO latestSummary(Long conversationId) {
        LambdaQueryWrapper<ConversationSummaryDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversationSummaryDO::getConversationId, conversationId)
                .orderByDesc(ConversationSummaryDO::getUpdateTime)
                .last("LIMIT 1");
        return conversationSummaryMapper.selectOne(wrapper);
    }

    private void refreshSummaryIfNeeded(Long conversationId) {
        List<ConversationMessageDO> allMessages = allMessages(conversationId);
        if (allMessages.size() <= SUMMARY_TRIGGER_MESSAGE_COUNT) {
            return;
        }

        int summarizeEnd = Math.max(0, allMessages.size() - SUMMARY_KEEP_RECENT_COUNT);
        List<ConversationMessageDO> messagesToSummarize = allMessages.subList(0, summarizeEnd);
        if (messagesToSummarize.isEmpty()) {
            return;
        }

        ConversationSummaryDO existing = latestSummary(conversationId);
        Long lastMessageId = messagesToSummarize.get(messagesToSummarize.size() - 1).getId();
        if (existing != null && existing.getMessageUntilId() != null
                && existing.getMessageUntilId() >= lastMessageId) {
            return;
        }

        String history = formatHistory(messagesToSummarize, messagesToSummarize.size(), true);
        String oldSummary = existing != null ? existing.getSummary() : "";
        String summary = generateSummary(oldSummary, history);
        if (!StringUtils.hasText(summary)) {
            return;
        }

        ConversationSummaryDO summaryDO = existing != null ? existing : new ConversationSummaryDO();
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

    private List<ConversationMessageDO> allMessages(Long conversationId) {
        LambdaQueryWrapper<ConversationMessageDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversationMessageDO::getConversationId, conversationId)
                .orderByAsc(ConversationMessageDO::getCreateTime);
        return conversationMessageMapper.selectList(wrapper);
    }

    private String generateSummary(String oldSummary, String history) {
        try {
            String userPrompt = promptTemplateManager.renderText(SUMMARY_TEMPLATE, Map.of(
                    "summary", oldSummary != null ? oldSummary : "",
                    "history", history
            ));
            return chatCompletionPort.complete(List.of(
                    ChatCompletionPort.Message.system("你是会话摘要助手，只保留对后续问答有用的上下文。"),
                    ChatCompletionPort.Message.user(userPrompt)
            ));
        } catch (Exception e) {
            log.warn("会话摘要生成失败，将继续使用短期记忆: {}", e.getMessage());
            return oldSummary;
        }
    }

    private String formatHistory(List<ConversationMessageDO> messages, int limit, boolean includeAssistant) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }

        int from = Math.max(0, messages.size() - limit);
        List<ConversationMessageDO> selected = new ArrayList<>(messages.subList(from, messages.size()));
        StringBuilder builder = new StringBuilder();
        for (ConversationMessageDO message : selected) {
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

    private Long currentUserId() {
        Long userId = UserContextHolder.getUserId();
        return userId != null ? userId : 1L;
    }

    private String shortTitle(String question) {
        if (!StringUtils.hasText(question)) {
            return "新会话";
        }
        String normalized = question.trim().replaceAll("\\s+", " ");
        return normalized.length() > TITLE_MAX_LENGTH
                ? normalized.substring(0, TITLE_MAX_LENGTH)
                : normalized;
    }

    private int estimateTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return Math.max(1, text.length() / 2);
    }
}
