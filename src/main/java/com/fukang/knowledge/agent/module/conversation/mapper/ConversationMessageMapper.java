package com.fukang.knowledge.agent.module.conversation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fukang.knowledge.agent.module.conversation.model.entity.ConversationMessageEntity;

import java.util.Comparator;
import java.util.List;

/**
 * RAG 会话消息 Mapper。
 */
public interface ConversationMessageMapper extends BaseMapper<ConversationMessageEntity> {

    default long countByConversationId(Long conversationId) {
        return selectCount(new LambdaQueryWrapper<ConversationMessageEntity>()
                .eq(ConversationMessageEntity::getConversationId, conversationId));
    }

    default List<ConversationMessageEntity> findRecentByConversationId(Long conversationId, int limit) {
        List<ConversationMessageEntity> messages = selectList(new LambdaQueryWrapper<ConversationMessageEntity>()
                .eq(ConversationMessageEntity::getConversationId, conversationId)
                .orderByDesc(ConversationMessageEntity::getCreateTime)
                .last("LIMIT " + limit));
        return messages.stream()
                .sorted(Comparator.comparing(ConversationMessageEntity::getCreateTime))
                .toList();
    }

    default List<ConversationMessageEntity> findAllByConversationId(Long conversationId) {
        return selectList(new LambdaQueryWrapper<ConversationMessageEntity>()
                .eq(ConversationMessageEntity::getConversationId, conversationId)
                .orderByAsc(ConversationMessageEntity::getCreateTime));
    }
}
