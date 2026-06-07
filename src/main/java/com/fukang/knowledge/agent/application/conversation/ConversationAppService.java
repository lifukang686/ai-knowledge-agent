package com.fukang.knowledge.agent.application.conversation;

import com.fukang.knowledge.agent.application.conversation.port.ConversationMemoryRepository;
import com.fukang.knowledge.agent.application.conversation.result.ConversationListItemResult;
import com.fukang.knowledge.agent.application.conversation.result.ConversationMessageResult;
import com.fukang.knowledge.agent.common.context.UserContextHolder;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.ConversationDO;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.ConversationMessageDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * QA 会话应用服务，面向前端会话栏。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationAppService {

    private static final String STATUS_ACTIVE = "active";
    private static final int DEFAULT_LIMIT = 50;

    private final ConversationMemoryRepository conversationMemoryRepository;

    /**
     * 查询当前用户的 QA 会话列表。
     */
    public List<ConversationListItemResult> listConversations(Long knowledgeBaseId, Integer limit) {
        int queryLimit = limit == null || limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, DEFAULT_LIMIT);
        Long userId = currentUserId();
        List<ConversationListItemResult> results = conversationMemoryRepository
                .findConversationsByUser(userId, knowledgeBaseId, queryLimit)
                .stream()
                .map(this::toListItem)
                .toList();
        log.info("加载QA会话列表: userId={}, count={}", userId, results.size());
        return results;
    }

    /**
     * 创建空会话窗口。
     */
    @Transactional(rollbackFor = Exception.class)
    public ConversationListItemResult createConversation(Long knowledgeBaseId) {
        ConversationDO conversation = new ConversationDO();
        conversation.setUserId(currentUserId());
        conversation.setKnowledgeBaseId(knowledgeBaseId);
        conversation.setTitle("新会话");
        conversation.setStatus(STATUS_ACTIVE);
        conversationMemoryRepository.insertConversation(conversation);
        log.info("创建QA会话: conversationId={}, knowledgeBaseId={}", conversation.getId(), knowledgeBaseId);
        return toListItem(conversation);
    }

    /**
     * 查询会话消息，并校验用户归属。
     */
    public List<ConversationMessageResult> listMessages(Long conversationId) {
        ensureOwnedConversation(conversationId);
        List<ConversationMessageResult> messages = conversationMemoryRepository.findAllMessages(conversationId)
                .stream()
                .map(this::toMessage)
                .toList();
        log.info("加载QA会话消息: conversationId={}, count={}", conversationId, messages.size());
        return messages;
    }

    /**
     * 校验会话属于当前用户。
     */
    public ConversationDO ensureOwnedConversation(Long conversationId) {
        ConversationDO conversation = conversationMemoryRepository.findConversationById(conversationId);
        Long userId = currentUserId();
        if (conversation == null) {
            throw new BaseException(ErrorCodeEnum.NOT_FOUND);
        }
        if (conversation.getUserId() != null && !conversation.getUserId().equals(userId)) {
            throw new BaseException(ErrorCodeEnum.FORBIDDEN);
        }
        return conversation;
    }

    private ConversationListItemResult toListItem(ConversationDO conversation) {
        long messageCount = conversationMemoryRepository.countMessages(conversation.getId());
        return new ConversationListItemResult(
                conversation.getId(),
                conversation.getKnowledgeBaseId(),
                conversation.getTitle(),
                conversation.getStatus(),
                messageCount,
                conversation.getUpdateTime(),
                conversation.getCreateTime(),
                conversation.getUpdateTime()
        );
    }

    private ConversationMessageResult toMessage(ConversationMessageDO message) {
        return new ConversationMessageResult(
                message.getId(),
                message.getConversationId(),
                message.getRole(),
                message.getContent(),
                message.getRewrittenQuery(),
                message.getStatus(),
                message.getCreateTime(),
                message.getUpdateTime()
        );
    }

    private Long currentUserId() {
        Long userId = UserContextHolder.getUserId();
        return userId != null ? userId : 1L;
    }
}
