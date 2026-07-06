package com.fukang.knowledge.agent.module.conversation.service;

import com.fukang.knowledge.agent.module.conversation.mapper.ConversationMapper;
import com.fukang.knowledge.agent.module.conversation.mapper.ConversationMessageMapper;
import com.fukang.knowledge.agent.module.conversation.mapper.ConversationSummaryMapper;
import com.fukang.knowledge.agent.module.conversation.model.vo.ConversationListItemResult;
import com.fukang.knowledge.agent.module.conversation.model.vo.ConversationMessageResult;
import com.fukang.knowledge.agent.common.context.UserContextHolder;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.module.conversation.model.entity.ConversationEntity;
import com.fukang.knowledge.agent.module.conversation.model.entity.ConversationMessageEntity;
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
public class ConversationService {

    /**
     * 生效会话状态。
     */
    private static final String STATUS_ACTIVE = "active";
    /**
     * 默认会话查询数量。
     */
    private static final int DEFAULT_LIMIT = 50;

    private final ConversationMapper conversationMapper;
    private final ConversationMessageMapper conversationMessageMapper;
    private final ConversationSummaryMapper conversationSummaryMapper;

    /**
     * 查询当前用户的 QA 会话列表。
     */
    public List<ConversationListItemResult> listConversations(Long knowledgeBaseId, Integer limit) {
        int queryLimit = limit == null || limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, DEFAULT_LIMIT);
        Long userId = currentUserId();
        List<ConversationListItemResult> results = conversationMapper
                .findActiveByUser(userId, knowledgeBaseId, queryLimit)
                .stream()
                .map(this::toListItem)
                .toList();
        log.info("加载QA会话列表: userId={}, knowledgeBaseId={}, count={}",
                userId, knowledgeBaseId, results.size());
        return results;
    }

    /**
     * 创建空会话窗口。
     */
    @Transactional(rollbackFor = Exception.class)
    public ConversationListItemResult createConversation(Long knowledgeBaseId) {
        ConversationEntity conversation = new ConversationEntity();
        conversation.setUserId(currentUserId());
        conversation.setKnowledgeBaseId(knowledgeBaseId);
        conversation.setTitle("新会话");
        conversation.setStatus(STATUS_ACTIVE);
        conversationMapper.insert(conversation);
        log.info("创建QA会话: conversationId={}, knowledgeBaseId={}", conversation.getId(), knowledgeBaseId);
        return toListItem(conversation);
    }

    /**
     * 查询会话消息，并校验用户归属。
     */
    public List<ConversationMessageResult> listMessages(Long conversationId) {
        ensureOwnedConversation(conversationId);
        List<ConversationMessageResult> messages = conversationMessageMapper.findAllByConversationId(conversationId)
                .stream()
                .map(this::toMessage)
                .toList();
        log.info("加载QA会话消息: conversationId={}, count={}", conversationId, messages.size());
        return messages;
    }

    /**
     * 校验会话属于当前用户。
     */
    public ConversationEntity ensureOwnedConversation(Long conversationId) {
        ConversationEntity conversation = conversationMapper.selectById(conversationId);
        Long userId = currentUserId();
        if (conversation == null) {
            throw new BaseException(ErrorCodeEnum.NOT_FOUND);
        }
        if (conversation.getUserId() != null && !conversation.getUserId().equals(userId)) {
            throw new BaseException(ErrorCodeEnum.FORBIDDEN);
        }
        return conversation;
    }

    /**
     * 转换会话列表项。
     */
    private ConversationListItemResult toListItem(ConversationEntity conversation) {
        long messageCount = conversationMessageMapper.countByConversationId(conversation.getId());
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

    /**
     * 转换会话消息。
     */
    private ConversationMessageResult toMessage(ConversationMessageEntity message) {
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

    /**
     * 获取当前用户 ID，未登录时拒绝访问。
     */
    private Long currentUserId() {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new BaseException(ErrorCodeEnum.UNAUTHORIZED);
        }
        return userId;
    }
}
