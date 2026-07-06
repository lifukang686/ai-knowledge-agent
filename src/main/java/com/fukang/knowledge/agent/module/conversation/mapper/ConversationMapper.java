package com.fukang.knowledge.agent.module.conversation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fukang.knowledge.agent.module.conversation.model.entity.ConversationEntity;

import java.util.List;

/**
 * RAG 会话 Mapper。
 */
public interface ConversationMapper extends BaseMapper<ConversationEntity> {

    default List<ConversationEntity> findActiveByUser(Long userId, Long knowledgeBaseId, int limit) {
        LambdaQueryWrapper<ConversationEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversationEntity::getUserId, userId)
                .eq(ConversationEntity::getStatus, "active");
        if (knowledgeBaseId != null) {
            wrapper.eq(ConversationEntity::getKnowledgeBaseId, knowledgeBaseId);
        }
        wrapper.orderByDesc(ConversationEntity::getUpdateTime)
                .last("LIMIT " + limit);
        return selectList(wrapper);
    }
}
