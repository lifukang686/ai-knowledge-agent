package com.fukang.knowledge.agent.application.conversation;

import com.fukang.knowledge.agent.application.ai.port.ChatCompletionPort;
import com.fukang.knowledge.agent.application.conversation.port.ConversationMemoryRepository;
import com.fukang.knowledge.agent.common.context.UserContextHolder;
import com.fukang.knowledge.agent.infrastructure.ai.PromptTemplateManager;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.ConversationDO;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.ConversationMessageDO;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.ConversationSummaryDO;
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

    private final ConversationMemoryRepository conversationMemoryRepository;
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
        ConversationDO conversation = conversationMemoryRepository.findConversationById(conversationId);
        if (conversation == null || StringUtils.hasText(conversation.getTitle())) {
            return;
        }
        conversation.setTitle(shortTitle(question));
        conversationMemoryRepository.updateConversation(conversation);
    }

    /**
     * 获取已有会话；不存在时创建新会话。
     */
    private ConversationDO resolveConversation(Long conversationId, Long knowledgeBaseId, String question) {
        if (conversationId != null) {
            ConversationDO existing = conversationMemoryRepository.findConversationById(conversationId);
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
        conversationMemoryRepository.insertConversation(conversation);
        return conversation;
    }

    /**
     * 写入一条会话消息。
     */
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
        conversationMemoryRepository.insertMessage(message);
    }

    /**
     * 查询最近消息，供短期记忆使用。
     */
    private List<ConversationMessageDO> recentMessages(Long conversationId, int limit) {
        return conversationMemoryRepository.findRecentMessages(conversationId, limit);
    }

    /**
     * 查询最新的长期摘要。
     */
    private ConversationSummaryDO latestSummary(Long conversationId) {
        return conversationMemoryRepository.findLatestSummary(conversationId);
    }

    /**
     * 消息过多时压缩旧消息为摘要。
     */
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
            conversationMemoryRepository.insertSummary(summaryDO);
        } else {
            conversationMemoryRepository.updateSummary(summaryDO);
        }
    }

    /**
     * 查询会话全部消息。
     */
    private List<ConversationMessageDO> allMessages(Long conversationId) {
        return conversationMemoryRepository.findAllMessages(conversationId);
    }

    /**
     * 调用模型生成新的会话摘要。
     */
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

    /**
     * 将消息列表格式化为 Prompt 历史文本。
     */
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

    /**
     * 获取当前用户，未登录时使用默认用户。
     */
    private Long currentUserId() {
        Long userId = UserContextHolder.getUserId();
        return userId != null ? userId : 1L;
    }

    /**
     * 使用首轮问题生成会话标题。
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
     */
    private int estimateTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return Math.max(1, text.length() / 2);
    }
}
